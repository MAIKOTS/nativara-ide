package app.controladores;

import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import app.modelo.clone.CloneConfig;
import app.modelo.clone.ResultadoClone;
import app.util.GerenciadorGit;

/**
 * Orquestra o fluxo de clone:
 *  - roda o GerenciadorGit em background
 *  - publica os callbacks na thread de UI
 *  - permite cancelamento
 *
 * Não sabe nada sobre Android Views nem sobre JGit.
 */
public class ControladorClone {

    public interface Callbacks {
        void aoLog(String mensagem);
        void aoIniciarEtapa(String titulo);
        void aoSucesso(File pasta, boolean usaGitLfs);
        void aoCancelar();
        void aoErro(Exception e);
    }

    private final Handler handlerUI = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final GerenciadorGit gerenciadorGit = new GerenciadorGit();

    private volatile boolean cancelado = false;
    private volatile boolean emAndamento = false;

    // ========================================================================
    // API pública
    // ========================================================================

    public boolean isEmAndamento() {
        return emAndamento;
    }

    public void cancelar() {
        if (emAndamento) cancelado = true;
    }

    public void clonar(final CloneConfig cfg, final Callbacks cb) {
        if (emAndamento) {
            if (cb != null) cb.aoLog("⚠ Já existe um clone em andamento.");
            return;
        }

        emAndamento = true;
        cancelado = false;

        executor.execute(() -> {
            try {
                ResultadoClone resultado = gerenciadorGit.clonar(
                        cfg,
                        titulo -> post(cb, () -> cb.aoIniciarEtapa(titulo)),
                        () -> cancelado
                );

                postResultado(resultado, cb);

            } catch (Exception e) {
                post(cb, () -> cb.aoErro(e));
            } finally {
                emAndamento = false;
            }
        });
    }

    /** Encerra o executor. Chame quando a tela for destruída. */
    public void shutdown() {
        executor.shutdownNow();
    }

    // ========================================================================
    // Internos
    // ========================================================================

    private void postResultado(ResultadoClone r, Callbacks cb) {
        switch (r.status) {
            case SUCESSO:
                post(cb, () -> cb.aoSucesso(r.pastaDestino, r.usaGitLfs));
                break;
            case CANCELADO:
                post(cb, () -> cb.aoCancelar());
                break;
            case ERRO:
                post(cb, () -> cb.aoErro(r.erro));
                break;
        }
    }

    private void post(Callbacks cb, Runnable r) {
        if (cb == null) return;
        handlerUI.post(r);
    }
}