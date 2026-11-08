package ui.telas;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import app.R;
import modelo.Caminhos;
import modelo.GerenciadorProjetoProperties;
import modelo.Projeto;
import ui.dialogos.DialogoApp;
import app.util.GerenciadorDeArquivos;

public class TelaNovoProjeto {

    public interface Acoes {
        void aoVoltar();
        void aoCriarProjeto(File pastaProjeto);
    }

    private final Context ctx;
    private final Acoes acoes;
    private final Handler handlerUI = new Handler(Looper.getMainLooper());

    private View raizView;
    private EditText campoNome, campoAutor, campoDescricao, campoVersao;
    private EditText campoFlags, campoFontes, campoIncludes, campoLibs, campoDependencias;
    private TextView campoApi, campoStd, campoOtimizacao;
    private TextView chipArm64, chipArm32;
    private TextView btnCriar;
    private TextView status;

    private boolean abiArm64Ativa = true;
    private boolean abiArm32Ativa = true;

    public TelaNovoProjeto(Context ctx, Acoes acoes) {
        this.ctx = ctx;
        this.acoes = acoes;
    }

    public View construir() {
        raizView = LayoutInflater.from(ctx)
                .inflate(R.layout.tela_novo_projeto, null, false);

        campoNome         = raizView.findViewById(R.id.novoProjetoNome);
        campoAutor        = raizView.findViewById(R.id.novoProjetoAutor);
        campoDescricao    = raizView.findViewById(R.id.novoProjetoDescricao);
        campoVersao       = raizView.findViewById(R.id.novoProjetoVersao);
        campoApi          = raizView.findViewById(R.id.novoProjetoApi);
        campoStd          = raizView.findViewById(R.id.novoProjetoStd);
        campoOtimizacao   = raizView.findViewById(R.id.novoProjetoOtimizacao);
        campoFlags        = raizView.findViewById(R.id.novoProjetoFlags);
        campoFontes       = raizView.findViewById(R.id.novoProjetoFontes);
        campoIncludes     = raizView.findViewById(R.id.novoProjetoIncludes);
        campoLibs         = raizView.findViewById(R.id.novoProjetoLibs);
        campoDependencias = raizView.findViewById(R.id.novoProjetoDependencias);
        chipArm64         = raizView.findViewById(R.id.novoProjetoAbiArm64);
        chipArm32         = raizView.findViewById(R.id.novoProjetoAbiArm32);
        btnCriar          = raizView.findViewById(R.id.novoProjetoBtnCriar);
        status            = raizView.findViewById(R.id.novoProjetoStatus);

        campoVersao.setText("1.0.0");
        campoFlags.setText("");
        campoIncludes.setText("include,mod,src");

        atualizarChipsAbi();

        raizView.findViewById(R.id.novoProjetoBtnVoltar).setOnClickListener(v -> {
            if (acoes != null) acoes.aoVoltar();
        });

        chipArm64.setOnClickListener(v -> {
            abiArm64Ativa = !abiArm64Ativa;
            garantirPeloMenosUmaAbi();
            atualizarChipsAbi();
        });

        chipArm32.setOnClickListener(v -> {
            abiArm32Ativa = !abiArm32Ativa;
            garantirPeloMenosUmaAbi();
            atualizarChipsAbi();
        });

        campoStd.setOnClickListener(v -> DialogoApp.selecaoUnica(ctx,
                "Padrão C++",
                new String[]{"c++11", "c++14", "c++17", "c++20", "c++23"},
                2, (pos, valor) -> campoStd.setText(valor)));

        campoOtimizacao.setOnClickListener(v -> DialogoApp.selecaoUnica(ctx,
                "Otimização",
                new String[]{"O0", "O1", "O2", "O3", "Os", "Oz"},
                2, (pos, valor) -> campoOtimizacao.setText(valor)));

        campoApi.setOnClickListener(v -> DialogoApp.selecaoUnica(ctx,
                "API mínima",
                new String[]{"21  (Android 5.0+)"},
                0, (pos, valor) -> campoApi.setText(valor)));

        btnCriar.setOnClickListener(v -> criarProjeto());

        return raizView;
    }

    private void garantirPeloMenosUmaAbi() {
        if (!abiArm64Ativa && !abiArm32Ativa) {
            abiArm64Ativa = true;
        }
    }

    private void atualizarChipsAbi() {
        pintarChip(chipArm64, abiArm64Ativa);
        pintarChip(chipArm32, abiArm32Ativa);
    }

    private void pintarChip(TextView chip, boolean ativo) {
        if (chip == null) return;

        GradientDrawable fundo = new GradientDrawable();
        fundo.setShape(GradientDrawable.RECTANGLE);
        fundo.setCornerRadius(dp(6));

        if (ativo) {
            fundo.setColor(Color.parseColor("#0D2A19"));
            fundo.setStroke(dp(1), Color.parseColor("#00E676"));
            chip.setTextColor(Color.parseColor("#00E676"));
        } else {
            fundo.setColor(Color.parseColor("#0F0F12"));
            fundo.setStroke(dp(1), Color.parseColor("#27272A"));
            chip.setTextColor(Color.parseColor("#52525B"));
        }
        chip.setBackground(fundo);
    }

    private void criarProjeto() {
        String nome = campoNome.getText().toString().trim();
        if (nome.isEmpty()) {
            erro("Digite um nome para o projeto.");
            campoNome.requestFocus();
            return;
        }

        nome = sanitizarNome(nome);
        if (nome.isEmpty()) {
            erro("Nome inválido. Use apenas letras, números, '-' e '_'.");
            campoNome.requestFocus();
            return;
        }

        String autor = campoAutor.getText().toString().trim();
        if (autor.isEmpty()) autor = "Desconhecido";

        String descricao = campoDescricao.getText().toString().trim();
        String versao = campoVersao.getText().toString().trim();
        if (versao.isEmpty()) versao = "1.0.0";

        File pastaProjeto = Caminhos.projeto(nome);
        if (pastaProjeto.exists()) {
            erro("Já existe um projeto com esse nome.");
            campoNome.requestFocus();
            return;
        }

        Projeto p = new Projeto();
        p.pastaRaiz = pastaProjeto;
        p.nome      = nome;
        p.versao    = versao;
        p.autor     = autor;
        p.descricao = descricao;
        p.libNome   = "lib" + nome.toLowerCase().replaceAll("[^a-z0-9_]", "_");

        p.abis = new ArrayList<>();
        if (abiArm64Ativa) p.abis.add("arm64-v8a");
        if (abiArm32Ativa) p.abis.add("armeabi-v7a");

        p.api = 21;
        p.std        = campoStd.getText().toString().trim();
        p.otimizacao = campoOtimizacao.getText().toString().trim();

        p.flags    = parseLista(campoFlags.getText().toString());
        p.fontes   = parseLista(campoFontes.getText().toString());
        p.includes = parseLista(campoIncludes.getText().toString());
        p.libs     = parseLista(campoLibs.getText().toString());
        p.dependencias = parseDependencias(
                campoDependencias.getText().toString(), pastaProjeto);

        btnCriar.setEnabled(false);
        btnCriar.setAlpha(0.5f);
        status.setText("Criando...");

        final Projeto projetoFinal = p;
        final String  nomeFinal    = nome;

        new Thread(() -> {
            boolean ok = construirEstrutura(projetoFinal);

            handlerUI.post(() -> {
                btnCriar.setEnabled(true);
                btnCriar.setAlpha(1.0f);

                if (ok) {
                    status.setText("Projeto criado!");
                    Toast.makeText(ctx,
                            "Projeto \"" + nomeFinal + "\" criado!",
                            Toast.LENGTH_SHORT).show();

                    if (acoes != null) {
                        acoes.aoCriarProjeto(projetoFinal.pastaRaiz);
                    }
                } else {
                    status.setText("");
                    erro("Falha ao criar o projeto. Verifique as permissões.");
                }
            });
        }, "criar-projeto").start();
    }

    private boolean construirEstrutura(Projeto p) {
        try {
            File raiz = p.pastaRaiz;
            if (!raiz.mkdirs() && !raiz.isDirectory()) return false;

            File src     = new File(raiz, Caminhos.SRC);
            File include = new File(raiz, Caminhos.INCLUDE);
            File deps    = new File(raiz, Caminhos.DEPS);
            File lib     = new File(raiz, Caminhos.LIB);

            src.mkdirs();
            include.mkdirs();
            deps.mkdirs();
            lib.mkdirs();

            for (String abi : p.abis) {
                String abiPasta = Projeto.normalizarAbi(abi);
                new File(lib, abiPasta).mkdirs();
            }

            if (!GerenciadorProjetoProperties.salvar(p)) return false;

            File mainCpp = new File(src, "main.cpp");
            String codigo =
                    "// " + p.nome + "\n" +
                    "// Mod AML para GTA San Andreas Android\n" +
                    "// Criado com a AML IDE\n\n" +
                    "#include <cstdio>\n" +
                    "#include <jni.h>\n\n" +
                    "extern \"C\" {\n" +
                    "    __attribute__((visibility(\"default\")))\n" +
                    "    void AMLMain() {\n" +
                    "        printf(\"Mod " + p.nome + " carregado!\\n\");\n" +
                    "    }\n" +
                    "}\n";

            GerenciadorDeArquivos.salvarArquivo(mainCpp, codigo);

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private List<String> parseLista(String texto) {
        List<String> out = new ArrayList<>();
        if (texto == null) return out;
        for (String s : texto.split(",")) {
            String t = s.trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }

    private List<modelo.Dependencia> parseDependencias(String texto, File pastaProjeto) {
        List<modelo.Dependencia> out = new ArrayList<>();
        if (texto == null || texto.trim().isEmpty()) return out;

        for (String item : texto.split(",")) {
            String s = item.trim();
            if (s.isEmpty()) continue;

            modelo.Dependencia d = new modelo.Dependencia();

            int idx = s.lastIndexOf('@');
            if (idx > 0 && idx < s.length() - 1) {
                d.url = s.substring(0, idx);
                d.branch = s.substring(idx + 1);
            } else {
                d.url = s;
                d.branch = null;
            }

            String url = d.url;
            while (url.endsWith("/")) url = url.substring(0, url.length() - 1);
            if (url.endsWith(".git")) url = url.substring(0, url.length() - 4);
            int slash = url.lastIndexOf('/');
            d.nomeRepo = (slash >= 0) ? url.substring(slash + 1) : url;
            d.pastaLocal = new File(pastaProjeto, Caminhos.DEPS + "/" + d.nomeRepo);

            out.add(d);
        }
        return out;
    }

    private String sanitizarNome(String nome) {
        if (nome == null) return "";
        return nome.trim()
                .replaceAll("[\\\\/:*?\"<>|]", "")
                .replaceAll("\\s+", "_");
    }

    private void erro(String msg) {
        status.setText(msg);
        status.setTextColor(Color.parseColor("#FF5252"));
    }

    private int dp(int v) {
        return (int) (v * ctx.getResources().getDisplayMetrics().density);
    }
}