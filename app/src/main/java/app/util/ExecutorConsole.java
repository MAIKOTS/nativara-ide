package app.util;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import ui.util.GerenciadorLogsConsole;

public final class ExecutorConsole {

    private static final String TAG = "ExecutorConsole";
    private static final String RUNNER = "libconsole_runner.so";

    public static class Resultado {
        public int    codigoSaida;
        public String stdout = "";
        public String stderr = "";
        public long   duracaoMs;
    }

    private ExecutorConsole() { }

    public static Resultado executar(Context ctx,
                                     File libPrograma,
                                     String[] args,
                                     String stdin,
                                     GerenciadorLogsConsole log) throws Exception {

        if (libPrograma == null || !libPrograma.isFile()) {
            throw new IllegalArgumentException("Programa não encontrado: " + libPrograma);
        }

        File runner = GerenciadorNDK.obterConsoleRunner(ctx);
        if (!runner.isFile()) {
            throw new IllegalStateException(
                    "console_runner não encontrado em " + runner.getParent());
        }

        String nativeDir = ctx.getApplicationInfo().nativeLibraryDir;

        // ★ Copia para filesDir (permite dlopen)
        File libExec = copiarParaExec(ctx, libPrograma, log);

        try {
            List<String> cmd = new ArrayList<>();
            cmd.add(runner.getAbsolutePath());
            cmd.add(libExec.getAbsolutePath());
            if (args != null) {
                for (String a : args) cmd.add(a);
            }

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(false);

            // ★ LD_LIBRARY_PATH inclui filesDir/exec + nativeDir + libsExtras
            File pastaExec = libExec.getParentFile();
            String ldPath = pastaExec.getAbsolutePath() + ":" + nativeDir;

            File libsExtras = GerenciadorNDK.obterLibsExtras(ctx);
            if (libsExtras != null) {
                ldPath = libsExtras.getAbsolutePath() + ":" + ldPath;
            }
            pb.environment().put("LD_LIBRARY_PATH", ldPath);

            if (log != null) {
                log.debug("Exec", "Comando: " + String.join(" ", cmd));
                log.debug("Env", "LD_LIBRARY_PATH = " + ldPath);
            }

            long inicio = System.currentTimeMillis();
            Process p = pb.start();

            if (stdin != null) {
                try (OutputStream out = p.getOutputStream()) {
                    out.write(stdin.getBytes("UTF-8"));
                }
            } else {
                p.getOutputStream().close();
            }

            StringBuilder sbOut = new StringBuilder();
            StringBuilder sbErr = new StringBuilder();

            Thread tOut = new Thread(() -> ler(p.getInputStream(), sbOut), "stdout-reader");
            Thread tErr = new Thread(() -> ler(p.getErrorStream(), sbErr), "stderr-reader");
            tOut.start();
            tErr.start();

            int codigo = p.waitFor();
            tOut.join();
            tErr.join();
            long duracao = System.currentTimeMillis() - inicio;

            Resultado r = new Resultado();
            r.codigoSaida = codigo;
            r.stdout      = sbOut.toString();
            r.stderr      = sbErr.toString();
            r.duracaoMs   = duracao;

            if (log != null) {
                log.info("Exec", "Exit code: " + codigo
                        + "  •  Duração: " + GerenciadorLogsConsole.formatarDuracao(duracao));
            }

            return r;

        } finally {
            if (libExec != null && libExec.exists()) {
                //noinspection ResultOfMethodCallIgnored
                libExec.delete();
            }
        }
    }

    // ==========================================================
    //  Helpers
    // ==========================================================

    private static File copiarParaExec(Context ctx,
                                       File origem,
                                       GerenciadorLogsConsole log) throws Exception {

        File pastaExec = new File(ctx.getFilesDir(), "exec");
        if (!pastaExec.exists()) pastaExec.mkdirs();

        String nomeUnico = "user_prog_" + System.currentTimeMillis()
                + "_" + origem.getName();
        File destino = new File(pastaExec, nomeUnico);

        if (log != null) {
            log.debug("Exec", "Copiando " + origem.getName()
                    + " → " + destino.getName());
        }

        try (InputStream in  = new FileInputStream(origem);
             OutputStream out = new FileOutputStream(destino)) {
            byte[] buf = new byte[65536];
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
            out.flush();
        }

        destino.setReadable(true, false);

        return destino;
    }

    private static void ler(InputStream in, StringBuilder sb) {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(in, "UTF-8"))) {
            String linha;
            while ((linha = br.readLine()) != null) {
                sb.append(linha).append("\n");
            }
        } catch (Exception e) {
            Log.w(TAG, "Erro lendo stream: " + e.getMessage());
        }
    }
}