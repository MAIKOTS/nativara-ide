package ui.telas;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import app.R;
import modelo.Caminhos;
import modelo.Projeto;
import ui.componentes.ConsoleTerminal;
import ui.dialogos.DialogoApp;
import ui.util.Cores;
import app.util.CompiladorNativo;
import app.util.ExecutorConsole;
import app.util.GerenciadorDeArquivos;
import ui.util.GerenciadorLogsConsole;
import app.util.GerenciadorNDK;

public class FerramentasDaIDE {

    public interface Acoes {
        void aoVoltar();
        void aoAbrirExploradorFiles();
        void aoAbrirRecursos();
    }

    private final Context ctx;
    private final Acoes acoes;
    private final Handler handlerUI = new Handler(Looper.getMainLooper());

    private View raizView;
    private ConsoleTerminal console;
    private TextView statusTopo;
    private TextView statusDetalhe;
    private ImageView statusIcone;
    private LinearLayout btnPreparar;
    private LinearLayout btnCompilar;
    private LinearLayout btnInspecionar;

    private final AtomicBoolean operacaoEmAndamento = new AtomicBoolean(false);

    public FerramentasDaIDE(Context ctx, Acoes acoes) {
        this.ctx = ctx;
        this.acoes = acoes;
    }

    public View construir() {
        raizView = LayoutInflater.from(ctx)
                .inflate(R.layout.tela_ferramentas_ide, null, false);

        console        = raizView.findViewById(R.id.ferramentasConsole);
        statusTopo     = raizView.findViewById(R.id.ferramentasStatus);
        statusDetalhe  = raizView.findViewById(R.id.ferramentasStatusDetalhe);
        statusIcone    = raizView.findViewById(R.id.ferramentasStatusIcone);
        btnPreparar    = raizView.findViewById(R.id.btnFerramentasPreparar);
        btnCompilar    = raizView.findViewById(R.id.btnFerramentasCompilar);
        btnInspecionar = raizView.findViewById(R.id.btnFerramentasInspecionar);

        raizView.findViewById(R.id.btnFerramentasRecursos)
                .setOnClickListener(v -> {
                    if (acoes != null) acoes.aoAbrirRecursos();
                });

        raizView.findViewById(R.id.btnFerramentasVoltar).setOnClickListener(v -> {
            if (operacaoEmAndamento.get()) {
                log("! Operação em andamento. Aguarde o fim.");
                return;
            }
            if (acoes != null) acoes.aoVoltar();
        });

        btnPreparar.setOnClickListener(v -> verificarToolchain());

        // ★★★★★ TEMPORÁRIO — teste do modo console ★★★★★
        // Quando terminar o teste, troque por: iniciarCompilacao()
        btnCompilar.setOnClickListener(v -> testarModoConsole());

        btnInspecionar.setOnClickListener(v -> {
            if (acoes != null) acoes.aoAbrirExploradorFiles();
        });

        atualizarStatusTopo();
        reabilitarBotoes();

        log("Ferramentas da IDE iniciadas.");
        log("ABI: " + GerenciadorNDK.obterAbi(ctx));
        log("Status: " + (GerenciadorNDK.estaPronta(ctx)
                ? "toolchain pronta" : "toolchain não instalada"));

        return raizView;
    }

    // ==========================================================
    //  Verificar toolchain
    // ==========================================================

    private void verificarToolchain() {
        operacaoEmAndamento.set(true);
        travarBotoesDuranteOperacao();
        atualizarStatusTopo();

        console.secao("VERIFICAÇÃO DA TOOLCHAIN");

        File sysroot = GerenciadorNDK.obterSysroot(ctx);
        File libs    = GerenciadorNDK.obterLibsExtras(ctx);

        boolean temSysroot = (sysroot != null && sysroot.isDirectory());
        boolean temLibs    = (libs    != null && libs.isDirectory());

        if (temSysroot) {
            log("✓ Sysroot:  " + Caminhos.exibir(sysroot));
        } else {
            log("✗ Sysroot não instalado");
        }

        if (temLibs) {
            log("✓ Libs:     " + Caminhos.exibir(libs));
        } else {
            log("✗ Libs extras não instaladas");
        }

        log("");
        log("ABI: " + GerenciadorNDK.obterAbi(ctx));

        operacaoEmAndamento.set(false);
        reabilitarBotoes();
        atualizarStatusTopo();

        if (temSysroot && temLibs) {
            console.sucesso("Toolchain completa e pronta para compilar.");
            return;
        }

        console.erro("Toolchain incompleta.");
        log("");
        log("Baixe os recursos em:");
        log("   Menu → Ferramentas da IDE → Recursos");

        DialogoApp.confirmar(ctx,
                "Baixar recursos?",
                "A toolchain não está completa.\n\n" +
                "Deseja abrir a tela de Recursos agora para baixar?",
                "Abrir Recursos", "Cancelar", false,
                () -> {
                    if (acoes != null) acoes.aoAbrirRecursos();
                });
    }

    // ==========================================================
    //  Compilação de teste (modo MOD — comportamento antigo)
    // ==========================================================

    private void iniciarCompilacao() {
        File pastaTeste = Caminhos.testeBuild(ctx);
        File pastaOut   = Caminhos.out(ctx);
        File pastaSrc   = new File(pastaTeste, Caminhos.SRC);
        pastaSrc.mkdirs();

        File fonte = new File(pastaSrc, "main.cpp");
        String codigo =
                "// Teste de compilação da AML IDE\n" +
                "#include <cstdio>\n\n" +
                "extern \"C\" void AMLMain() {\n" +
                "    printf(\"Ola do AML IDE!\\n\");\n" +
                "}\n";

        if (!GerenciadorDeArquivos.salvarArquivo(fonte, codigo)) {
            log("✗ Não foi possível criar main.cpp de teste.");
            return;
        }

        File saida = new File(pastaOut, "libmain.so");

        operacaoEmAndamento.set(true);
        travarBotoesDuranteOperacao();
        atualizarStatusTopo();

        GerenciadorLogsConsole logDetalhado = new GerenciadorLogsConsole(ctx, console);

        CompiladorNativo.compilarParaSo(ctx, fonte, saida,
                new CompiladorNativo.ResultadoCompilacao() {
                    @Override
                    public void aoSucesso(File arquivoSo) {
                        handlerUI.post(() -> {
                            operacaoEmAndamento.set(false);
                            reabilitarBotoes();
                            atualizarStatusTopo();
                        });
                    }

                    @Override
                    public void aoErro(String mensagemErro) {
                        handlerUI.post(() -> {
                            operacaoEmAndamento.set(false);
                            reabilitarBotoes();
                            atualizarStatusTopo();
                        });
                    }
                },
                logDetalhado);
    }

    // ==========================================================
    //  Teste do modo CONSOLE (runner + dlopen)
    // ==========================================================

    private void testarModoConsole() {
        // ---------- 1. Cria um programa console simples ----------
        File pastaTeste = Caminhos.testeBuild(ctx);
        File pastaSrc   = new File(pastaTeste, Caminhos.SRC);
        pastaSrc.mkdirs();

        File fonte = new File(pastaSrc, "console_main.cpp");
        String codigo =
                "#include <cstdio>\n" +
                "int main(int argc, char** argv) {\n" +
                "    printf(\"Ola do console!\\n\");\n" +
                "    printf(\"argc = %d\\n\", argc);\n" +
                "    for (int i = 0; i < argc; i++) {\n" +
                "        printf(\"  argv[%d] = %s\\n\", i, argv[i]);\n" +
                "    }\n" +
                "    fprintf(stderr, \"isso vai pro stderr\\n\");\n" +
                "    return 42;\n" +
                "}\n";

        if (!GerenciadorDeArquivos.salvarArquivo(fonte, codigo)) {
            log("✗ Não foi possível criar console_main.cpp");
            return;
        }

        // ---------- 2. Monta Projeto CONSOLE ----------
        Projeto p = new Projeto();
        p.pastaRaiz = pastaTeste;
        p.nome      = "teste_console";
        p.libNome   = "libteste_console";
        p.tipoBuild = Projeto.TipoBuild.CONSOLE;

        p.abis = new ArrayList<>();
        p.abis.add("arm64-v8a");

        operacaoEmAndamento.set(true);
        travarBotoesDuranteOperacao();

        final GerenciadorLogsConsole logDetalhado =
                new GerenciadorLogsConsole(ctx, console);

        // ---------- 3. Compila ----------
        CompiladorNativo.compilarProjeto(ctx, p,
                new CompiladorNativo.ResultadoCompilacao() {

                    @Override
                    public void aoSucesso(File arquivoSo) {
                        // ---------- 4. Executa em thread separada ----------
                        new Thread(() -> {
                            try {
                                ExecutorConsole.Resultado r =
                                        ExecutorConsole.executar(
                                                ctx,
                                                arquivoSo,
                                                new String[]{"alpha", "beta"},
                                                null,
                                                logDetalhado);

                                handlerUI.post(() -> {
                                    console.secao("RESULTADO DO RUNNER");
                                    console.info("Exit code: " + r.codigoSaida);

                                    if (!r.stdout.isEmpty()) {
    console.secao("stdout");
    console.cru(r.stdout);
}
if (!r.stderr.isEmpty()) {
    console.secao("stderr");
    console.cru(r.stderr);
}

                                    operacaoEmAndamento.set(false);
                                    reabilitarBotoes();
                                });

                            } catch (Exception e) {
                                handlerUI.post(() -> {
                                    log("✗ Exceção ao executar: " + e.getMessage());
                                    e.printStackTrace();
                                    operacaoEmAndamento.set(false);
                                    reabilitarBotoes();
                                });
                            }
                        }, "teste-console").start();
                    }

                    @Override
                    public void aoErro(String mensagemErro) {
                        handlerUI.post(() -> {
                            operacaoEmAndamento.set(false);
                            reabilitarBotoes();
                        });
                    }
                },
                logDetalhado);
    }

    // ==========================================================
    //  Estado / UI helpers
    // ==========================================================

    private void travarBotoesDuranteOperacao() {
        setCardHabilitado(btnPreparar, false);
        setCardHabilitado(btnCompilar, false);
        setCardHabilitado(btnInspecionar, false);
    }

    private void reabilitarBotoes() {
        boolean pronta = GerenciadorNDK.estaPronta(ctx);

        setCardHabilitado(btnPreparar, !pronta);
        setCardHabilitado(btnCompilar, pronta);
        setCardHabilitado(btnInspecionar, true);
    }

    private void setCardHabilitado(View card, boolean habilitado) {
        if (card == null) return;
        card.setEnabled(habilitado);
        card.setClickable(habilitado);
        card.setAlpha(habilitado ? 1.0f : 0.4f);
    }

    private void atualizarStatusTopo() {
        if (operacaoEmAndamento.get()) {
            aplicarStatus("Operação em andamento...",
                    "Aguarde a conclusão", Cores.AMARELO);
            return;
        }

        if (GerenciadorNDK.estaPronta(ctx)) {
            aplicarStatus("Toolchain pronta",
                    "Clang 14 • NDK r24 • ABI " + GerenciadorNDK.obterAbi(ctx),
                    Cores.VERDE);
        } else {
            aplicarStatus("Toolchain não instalada",
                    "Baixe os recursos em Ferramentas da IDE → Recursos",
                    Cores.AMARELO);
        }
    }

    private void aplicarStatus(String titulo, String detalhe, int cor) {
        if (statusTopo != null) statusTopo.setText(titulo);
        if (statusDetalhe != null) statusDetalhe.setText(detalhe);
        if (statusIcone != null) {
            statusIcone.setColorFilter(cor);
            if (cor == Cores.VERDE) {
                statusIcone.setImageResource(R.drawable.ic_pasta_aberta);
            } else {
                statusIcone.setImageResource(R.drawable.ic_config);
            }
        }
    }

    private void log(String linha) {
        if (console == null) return;
        console.info(linha);
    }
}