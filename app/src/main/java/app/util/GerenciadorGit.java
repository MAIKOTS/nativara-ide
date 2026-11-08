package app.util;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;

import app.modelo.clone.CloneConfig;
import app.modelo.clone.ResultadoClone;

/**
 * Lógica pura de clonagem Git. Não depende de Android — pode ser
 * testada com JUnit puro.
 *
 * A thread que chama este método é responsável por rodar em background;
 * esta classe não cria threads.
 */
public class GerenciadorGit {

    /** Interface para checagem de cancelamento durante o clone. */
    public interface CancelamentoChecker {
        boolean isCancelado();
    }

    /** Interface para receber eventos durante o clone. */
    public interface ProgressoCallback {
        void aoIniciarEtapa(String titulo);
    }

    // ========================================================================
    // Clone
    // ========================================================================

    /**
     * Executa o clone em modo raso (depth=1), sem tags, apenas a branch
     * especificada (ou a padrão, se branch for vazio).
     *
     * NUNCA lança exceção — sempre retorna um {@link ResultadoClone}.
     */
    public ResultadoClone clonar(CloneConfig cfg,
                                 ProgressoCallback callback,
                                 CancelamentoChecker cancelChecker) {

        if (cfg == null || cfg.destino == null) {
            return ResultadoClone.erro(null,
                    new IllegalArgumentException("CloneConfig inválido"));
        }

        try {
            Git.cloneRepository()
                    .setURI(normalizarUrl(cfg.url))
                    .setDirectory(cfg.destino)
                    .setDepth(1)
                    .setCloneAllBranches(false)
                    .setBranch(cfg.temBranch() ? cfg.branch : null)
                    .setBranchesToClone(cfg.temBranch()
                            ? Collections.singletonList("refs/heads/" + cfg.branch)
                            : null)
                    .setNoTags()
                    .setProgressMonitor(new ProgressMonitor() {
                        @Override public void start(int totalTasks) {}

                        @Override public void beginTask(String title, int totalWork) {
                            if (callback != null
                                    && title != null
                                    && !title.isEmpty()) {
                                callback.aoIniciarEtapa(title);
                            }
                        }

                        @Override public void update(int completed) {}
                        @Override public void endTask() {}

                        @Override public boolean isCancelled() {
                            return cancelChecker != null && cancelChecker.isCancelado();
                        }

                        @Override public void showDuration(boolean enabled) {}
                    })
                    .setCredentialsProvider(cfg.temToken()
                            ? new UsernamePasswordCredentialsProvider("token", cfg.token)
                            : null)
                    .call();

            if (cancelChecker != null && cancelChecker.isCancelado()) {
                return ResultadoClone.cancelado(cfg.destino);
            }

            boolean usaLfs = detectarLFS(cfg.destino);
            return ResultadoClone.sucesso(cfg.destino, usaLfs);

        } catch (GitAPIException e) {
            if (cancelChecker != null && cancelChecker.isCancelado()) {
                return ResultadoClone.cancelado(cfg.destino);
            }
            return ResultadoClone.erro(cfg.destino, e);
        }
    }

    // ========================================================================
    // Detecção de Git LFS
    // ========================================================================

    /**
     * Verifica se o repositório clonado usa Git LFS, lendo o .gitattributes.
     */
    public boolean detectarLFS(File pastaRepo) {
        if (pastaRepo == null) return false;

        File gitattributes = new File(pastaRepo, ".gitattributes");
        if (!gitattributes.exists()) return false;

        try {
            String conteudo = new String(
                    Files.readAllBytes(gitattributes.toPath()),
                    StandardCharsets.UTF_8);
            return conteudo.contains("filter=lfs");
        } catch (IOException e) {
            return false;
        }
    }

    // ========================================================================
    // Helpers estáticos (podem ser usados pela UI para pré-validação)
    // ========================================================================

    /**
     * Verifica se a URL tem um esquema suportado.
     */
    public static boolean urlValida(String url) {
        if (url == null) return false;
        String s = url.trim().toLowerCase();
        return s.startsWith("https://")
                || s.startsWith("http://")
                || s.startsWith("git@")
                || s.startsWith("ssh://");
    }

    /**
     * Garante que URLs HTTPS terminem em .git.
     * URLs SSH (git@...) são mantidas como estão.
     */
    public static String normalizarUrl(String url) {
        if (url == null) return "";
        String s = url.trim();
        if (s.startsWith("git@")) return s;
        if (s.endsWith(".git")) return s;
        while (s.endsWith("/")) s = s.substring(0, s.length() - 1);
        return s + ".git";
    }

    /**
     * Extrai o nome do repositório a partir de uma URL.
     *
     *   https://github.com/user/meu-repo.git  → meu-repo
     *   https://github.com/user/meu-repo      → meu-repo
     *   git@github.com:user/meu-repo.git      → meu-repo
     */
    public static String extrairNomeRepo(String url) {
        if (url == null) return "";
        String s = url.trim();
        while (s.endsWith("/")) s = s.substring(0, s.length() - 1);
        if (s.endsWith(".git")) s = s.substring(0, s.length() - 4);
        int idx = Math.max(s.lastIndexOf('/'), s.lastIndexOf(':'));
        if (idx < 0) return s;
        return s.substring(idx + 1);
    }

    /**
     * Substitui caracteres inválidos para nomes de pasta.
     */
    public static String sanitizarNome(String nome) {
        if (nome == null) return "";
        return nome.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    /**
     * Apaga arquivo ou pasta recursivamente. Silencioso em caso de erro.
     */
    public static void apagarRecursivo(File f) {
        if (f == null || !f.exists()) return;
        if (f.isDirectory()) {
            File[] filhos = f.listFiles();
            if (filhos != null) {
                for (File filho : filhos) apagarRecursivo(filho);
            }
        }
        //noinspection ResultOfMethodCallIgnored
        f.delete();
    }
}