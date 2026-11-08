package app.modelo.clone;

import java.io.File;

/**
 * Dados de entrada para uma operação de clone.
 * Imutável — criado pela UI e consumido pelo GerenciadorGit.
 */
public class CloneConfig {

    public final String url;
    public final String branch;   // pode ser vazio
    public final String token;    // pode ser vazio
    public final File destino;

    public CloneConfig(String url, String branch, String token, File destino) {
        this.url = url;
        this.branch = branch;
        this.token = token;
        this.destino = destino;
    }

    public boolean temBranch() {
        return branch != null && !branch.trim().isEmpty();
    }

    public boolean temToken() {
        return token != null && !token.trim().isEmpty();
    }
}