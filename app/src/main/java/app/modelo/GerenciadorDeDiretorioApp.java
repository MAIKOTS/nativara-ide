package modelo;

import java.io.File;

/**
 * @deprecated Use {@link Caminhos#raiz()} — fonte única de verdade.
 */
@Deprecated
public final class GerenciadorDeDiretorioApp {

    private GerenciadorDeDiretorioApp() { }

    /** @deprecated Use {@link Caminhos#raiz()} */
    @Deprecated
    public static File obterOuCriarDiretorioProjetos() {
        return Caminhos.raiz();
    }
}