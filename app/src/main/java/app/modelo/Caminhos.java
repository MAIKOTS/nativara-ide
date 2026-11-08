package modelo;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;

/**
 * Fonte única de verdade para caminhos de arquivo do app.
 *
 *   Caminhos.raiz()                → /sdcard/AML-IDE
 *   Caminhos.projetos()            → /sdcard/AML-IDE/Projetos
 *   Caminhos.projeto("MeuMod")     → /sdcard/AML-IDE/Projetos/MeuMod (NÃO cria)
 *   Caminhos.github()              → /sdcard/AML-IDE/Github
 *   Caminhos.projetoGithub("repo") → /sdcard/AML-IDE/Github/repo (NÃO cria)
 *
 *   Caminhos.testeBuild(ctx)       → files/teste_build
 *   Caminhos.out(ctx)              → files/out
 *   Caminhos.recursos(ctx)         → files/recursos
 *   Caminhos.sysroot(ctx)          → files/sysroot
 *   Caminhos.libsExtras(ctx)       → files/lib
 *
 *   Caminhos.exibir(pasta)         → "AML-IDE/Projetos/MeuMod"
 *
 * Migra AML_IDE → AML-IDE automaticamente na primeira chamada.
 */
public final class Caminhos {

    private static final String TAG = "Caminhos";

    // ==========================================================
    //  Nomes canônicos
    // ==========================================================

    public static final String RAIZ     = "Nativara-IDE";
    public static final String PROJETOS = "Projetos";
    public static final String GITHUB   = "Clones_Github";

    public static final String TESTE_BUILD = "teste_build";
    public static final String OUT         = "out";
    public static final String RECURSOS    = "recursos";
    public static final String SYSROOT     = "sysroot";
    public static final String LIBS_EXTRAS = "lib";

    public static final String SRC     = "src";
    public static final String INCLUDE = "include";
    public static final String DEPS    = "deps";
    public static final String LIB     = "lib";

    /** Nome antigo (com underscore) — usado apenas na migração. */
    private static final String RAIZ_LEGADA = "Nativara-IDE";

    private static final Object LOCK = new Object();
    private static volatile boolean migracaoFeita = false;

    private Caminhos() { }

    // ==========================================================
    //  Externos (sdcard)
    // ==========================================================

    /** /sdcard/AML-IDE — cria se não existir. */
    public static File raiz() {
        migrarLegado();
        return garantir(new File(storageExterno(), RAIZ));
    }

    /** /sdcard/AML-IDE/Projetos — cria se não existir. */
    public static File projetos() {
        return garantir(new File(raiz(), PROJETOS));
    }

    /**
     * /sdcard/AML-IDE/Projetos/<nome>
     * NÃO cria a pasta — permite checar .exists() antes de criar projeto.
     */
    public static File projeto(String nome) {
        return new File(projetos(), nome);
    }

    /** /sdcard/AML-IDE/Github — cria se não existir. */
    public static File github() {
        return garantir(new File(raiz(), GITHUB));
    }

    /**
     * /sdcard/AML-IDE/Github/<nome>
     * NÃO cria a pasta.
     */
    public static File projetoGithub(String nome) {
        return new File(github(), nome);
    }

    // ==========================================================
    //  Internos do app (files/)
    // ==========================================================

    /** files/teste_build */
    public static File testeBuild(Context ctx) {
        return garantir(new File(ctx.getFilesDir(), TESTE_BUILD));
    }

    /** files/out */
    public static File out(Context ctx) {
        return garantir(new File(ctx.getFilesDir(), OUT));
    }

    /** files/recursos */
    public static File recursos(Context ctx) {
        return garantir(new File(ctx.getFilesDir(), RECURSOS));
    }

    /** files/sysroot */
    public static File sysroot(Context ctx) {
        return new File(ctx.getFilesDir(), SYSROOT);
    }

    /** files/lib */
    public static File libsExtras(Context ctx) {
        return new File(ctx.getFilesDir(), LIBS_EXTRAS);
    }

    // ==========================================================
    //  Exibição amigável
    // ==========================================================

    /** Remove o prefixo /storage/emulated/0/ quando presente. */
    public static String exibir(File pasta) {
        if (pasta == null) return "";
        String full = pasta.getAbsolutePath();
        String raiz = storageExterno().getAbsolutePath();
        if (full.startsWith(raiz + File.separator)) {
            return full.substring(raiz.length() + 1);
        }
        return full;
    }

    public static String exibirCompleto(File arquivo) {
        if (arquivo == null) return "";
        return exibir(arquivo.getParentFile()) + "/" + arquivo.getName();
    }

    // ==========================================================
    //  Internos
    // ==========================================================

    private static File storageExterno() {
        File sdcard = Environment.getExternalStorageDirectory();
        return sdcard != null ? sdcard : new File("/sdcard");
    }

    private static File garantir(File pasta) {
        if (!pasta.exists()) {
            //noinspection ResultOfMethodCallIgnored
            pasta.mkdirs();
        }
        return pasta;
    }

    private static void migrarLegado() {
        if (migracaoFeita) return;
        synchronized (LOCK) {
            if (migracaoFeita) return;
            migracaoFeita = true;

            File antiga = new File(storageExterno(), RAIZ_LEGADA);
            File nova   = new File(storageExterno(), RAIZ);

            if (!antiga.exists() || nova.exists()) return;

            if (antiga.renameTo(nova)) {
                Log.i(TAG, "Migrado: " + RAIZ_LEGADA + " → " + RAIZ);
            } else {
                Log.w(TAG, "Não foi possível migrar " + RAIZ_LEGADA
                        + " — o app usará " + RAIZ + " a partir de agora.");
            }
        }
    }
}