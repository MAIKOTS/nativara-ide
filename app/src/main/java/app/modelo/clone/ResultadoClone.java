package app.modelo.clone;

import java.io.File;

/**
 * Resultado de uma operação de clone.
 * Usado para passar informação da camada de dados para a UI.
 */
public class ResultadoClone {

    public enum Status {
        SUCESSO,
        CANCELADO,
        ERRO
    }

    public final Status status;
    public final File pastaDestino;
    public final Exception erro;
    public final boolean usaGitLfs;

    private ResultadoClone(Status status, File pasta, Exception erro, boolean usaLfs) {
        this.status = status;
        this.pastaDestino = pasta;
        this.erro = erro;
        this.usaGitLfs = usaLfs;
    }

    public static ResultadoClone sucesso(File pasta, boolean usaLfs) {
        return new ResultadoClone(Status.SUCESSO, pasta, null, usaLfs);
    }

    public static ResultadoClone cancelado(File pasta) {
        return new ResultadoClone(Status.CANCELADO, pasta, null, false);
    }

    public static ResultadoClone erro(File pasta, Exception e) {
        return new ResultadoClone(Status.ERRO, pasta, e, false);
    }

    public boolean isSucesso() {
        return status == Status.SUCESSO;
    }

    public boolean isCancelado() {
        return status == Status.CANCELADO;
    }

    public boolean isErro() {
        return status == Status.ERRO;
    }
}