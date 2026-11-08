package ui.telas;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

import app.R;
import app.controladores.ControladorClone;
import app.modelo.clone.CloneConfig;
import app.util.GerenciadorGit;
import modelo.Caminhos;

public class TelaClonarRepositorio {

    public interface Acoes {
        void aoVoltar();
        void aoClonarComSucesso(File pastaProjeto);
    }

    private final Context ctx;
    private final Acoes acoes;
    private final ControladorClone controlador = new ControladorClone();

    private EditText inputUrl;
    private EditText inputBranch;
    private EditText inputToken;
    private TextView console;
    private TextView btnIniciar;
    private TextView btnCancelar;
    private ProgressBar progressClone;
    private TextView txtStatusTopo;
    private TextView btnLimparConsole;
    private ScrollView scrollConsole;

    public TelaClonarRepositorio(Context ctx, Acoes acoes) {
        this.ctx = ctx;
        this.acoes = acoes;
    }

    public View construir() {
        View raiz = LayoutInflater.from(ctx)
                .inflate(R.layout.tela_clonar_repositorio, null, false);

        inputUrl    = raiz.findViewById(R.id.inputUrlRepositorio);
        inputBranch = raiz.findViewById(R.id.inputBranch);
        inputToken  = raiz.findViewById(R.id.inputToken);

        console          = raiz.findViewById(R.id.consoleClone);
        scrollConsole    = raiz.findViewById(R.id.scrollConsole);
        btnLimparConsole = raiz.findViewById(R.id.btnLimparConsole);

        btnIniciar    = raiz.findViewById(R.id.btnIniciarClone);
        btnCancelar   = raiz.findViewById(R.id.btnCancelarClone);
        progressClone = raiz.findViewById(R.id.progressClone);
        txtStatusTopo = raiz.findViewById(R.id.txtStatusTopo);

        raiz.findViewById(R.id.btnClonarVoltar).setOnClickListener(v -> {
            if (controlador.isEmAndamento()) {
                Toast.makeText(ctx,
                        "Aguarde o clone terminar ou cancele.",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            if (acoes != null) acoes.aoVoltar();
        });

        if (btnIniciar != null) {
            btnIniciar.setOnClickListener(v -> iniciarClone());
        }

        if (btnCancelar != null) {
            btnCancelar.setOnClickListener(v -> {
                if (controlador.isEmAndamento()) {
                    log("⚠ Cancelando clone...");
                    controlador.cancelar();
                }
            });
        }

        if (btnLimparConsole != null) {
            btnLimparConsole.setOnClickListener(v -> {
                if (console != null) console.setText("");
            });
        }

        atualizarEstadoClone(false);

        return raiz;
    }

    public void destruir() {
        controlador.cancelar();
        controlador.shutdown();
    }

    private void iniciarClone() {

        String url = inputUrl.getText().toString().trim();
        String branch = inputBranch.getText().toString().trim();
        String token = inputToken.getText().toString().trim();

        if (url.isEmpty()) {
            toast("Informe a URL do repositório.");
            return;
        }
        if (!GerenciadorGit.urlValida(url)) {
            toast("URL inválida. Use https:// ou git@");
            return;
        }

        String nomeRepo = GerenciadorGit.sanitizarNome(
                GerenciadorGit.extrairNomeRepo(url));
        if (nomeRepo.isEmpty()) {
            log("✗ Não foi possível extrair o nome do repositório.");
            return;
        }

        File pastaDestino = Caminhos.projetoGithub(nomeRepo);

        if (pastaDestino.exists()) {
            log("⚠ Já existe uma pasta com o nome '" + nomeRepo + "'.");
            log("Caminho: " + Caminhos.exibir(pastaDestino));
            log("");

            new android.app.AlertDialog.Builder(ctx)
                    .setTitle("Pasta já existe")
                    .setMessage("Deseja apagar a pasta existente e clonar de novo?")
                    .setPositiveButton("Apagar e re-clonar", (d, w) -> {
                        GerenciadorGit.apagarRecursivo(pastaDestino);
                        log("✓ Pasta antiga apagada. Iniciando novo clone...\n");
                        iniciarClone();
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
            return;
        }

        console.setText("Iniciando clone...\n");

        atualizarEstadoClone(true);
        atualizarStatus("CLONANDO", R.color.verde_destaque);
        log("Repositório: " + nomeRepo);
        log("Destino:     " + Caminhos.exibir(pastaDestino));
        log("URL:         " + url);
        if (!branch.isEmpty()) log("Branch:      " + branch);
        log("Modo:        clone raso (só último commit)");
        log("");
        log("Clonando...\n");

        CloneConfig cfg = new CloneConfig(url, branch, token, pastaDestino);

        controlador.clonar(cfg, new ControladorClone.Callbacks() {
            @Override public void aoLog(String m) { log(m); }

            @Override public void aoIniciarEtapa(String titulo) {
                log("→ " + titulo);
            }

            @Override public void aoSucesso(File pasta, boolean usaLfs) {
                log("\n✓ Clone concluído com sucesso!");
                log("Pasta: " + Caminhos.exibir(pasta));

                if (usaLfs) {
                    log("");
                    log("⚠ Este repositório usa Git LFS.");
                    log("   Os arquivos grandes (.so, .a, etc.) vieram");
                    log("   apenas como ponteiros e precisam ser baixados");
                    log("   separadamente com 'git lfs pull'.");
                }

                atualizarEstadoClone(false);
                atualizarStatus("CONCLUÍDO", R.color.verde_destaque);

                if (acoes != null) acoes.aoClonarComSucesso(pasta);
            }

            @Override public void aoCancelar() {
                log("\n⚠ Clone cancelado pelo usuário.");
                GerenciadorGit.apagarRecursivo(pastaDestino);

                atualizarEstadoClone(false);
                atualizarStatus("CANCELADO", R.color.texto_secundario);
            }

            @Override public void aoErro(Exception e) {
                log("\n✗ Erro ao clonar: " + e.getMessage());
                tratarErro(e);
                GerenciadorGit.apagarRecursivo(pastaDestino);

                atualizarEstadoClone(false);
                atualizarStatus("ERRO", android.R.color.holo_red_light);
            }
        });
    }

    private void atualizarEstadoClone(boolean clonando) {
        if (progressClone != null) {
            progressClone.setVisibility(clonando ? View.VISIBLE : View.GONE);
        }
        if (btnCancelar != null) {
            btnCancelar.setVisibility(clonando ? View.VISIBLE : View.GONE);
        }
        if (btnIniciar != null) {
            btnIniciar.setEnabled(!clonando);
            btnIniciar.setAlpha(clonando ? 0.5f : 1.0f);
        }

        if (inputUrl != null)    inputUrl.setEnabled(!clonando);
        if (inputBranch != null) inputBranch.setEnabled(!clonando);
        if (inputToken != null)  inputToken.setEnabled(!clonando);
    }

    private void atualizarStatus(String texto, int corRes) {
        if (txtStatusTopo == null) return;
        txtStatusTopo.setText(texto);
        txtStatusTopo.setTextColor(getColorCompat(corRes));
    }

    private void tratarErro(Exception e) {
        String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";

        if (msg.contains("not authorized") || msg.contains("authentication")
                || msg.contains("401")) {
            log("");
            log("Dica: repositórios privados precisam de um");
            log("Personal Access Token (PAT) do GitHub.");
            log("Cole-o no campo 'Token' acima.");
        } else if (msg.contains("not found") || msg.contains("404")) {
            log("");
            log("Dica: verifique se a URL está correta e");
            log("se o repositório existe / é público.");
        } else if (msg.contains("unable to resolve host")
                || msg.contains("network")
                || msg.contains("connection")) {
            log("");
            log("Dica: verifique sua conexão com a internet.");
        } else if (msg.contains("branch") || msg.contains("not found in")) {
            log("");
            log("Dica: a branch informada não existe.");
            log("Deixe o campo em branco para usar a padrão.");
        } else if (msg.contains("already exists")) {
            log("");
            log("Dica: já existe uma pasta com esse nome.");
        }
    }

    private void toast(String m) {
        Toast.makeText(ctx, m, Toast.LENGTH_SHORT).show();
    }

    private void log(String mensagem) {
        if (console == null) return;
        console.append(mensagem + "\n");

        if (scrollConsole != null) {
            scrollConsole.post(() -> scrollConsole.fullScroll(View.FOCUS_DOWN));
        }
    }

    private int getColorCompat(int resId) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            return ctx.getColor(resId);
        }
        //noinspection deprecation
        return ctx.getResources().getColor(resId);
    }
}