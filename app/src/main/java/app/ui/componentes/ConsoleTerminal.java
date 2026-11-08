package ui.componentes;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Looper;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import ui.util.Cores;

/**
 * Mini-terminal visual.
 *
 * ★ Thread-safe: pode ser chamado de qualquer thread.
 */
public class ConsoleTerminal extends ScrollView {

    private final LinearLayout container;
    private final SimpleDateFormat fmtHora =
            new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

    private boolean mostrarTimestamp = true;

    public ConsoleTerminal(Context ctx) {
        this(ctx, null);
    }

    public ConsoleTerminal(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
        setBackgroundColor(Cores.FUNDO_RAIZ);
        setVerticalScrollBarEnabled(true);
        int pad = dp(ctx, 12);
        setPadding(pad, pad, pad, pad);

        container = new LinearLayout(ctx);
        container.setOrientation(LinearLayout.VERTICAL);
        addView(container);

        appendInterno("$ ", Cores.PROMPT);
        appendInterno("console pronto\n", Cores.TEXTO_DIM);
    }

    // ==========================================================
    //  API pública — todas thread-safe
    // ==========================================================

    public void setMostrarTimestamp(boolean mostrar) {
        this.mostrarTimestamp = mostrar;
    }

    public void info(final String texto) {
        executarNoUi(() -> linhaInterno("  ", texto, Cores.TEXTO_PRIMARIO));
    }

    public void ok(final String texto) {
        executarNoUi(() -> linhaInterno("✓ ", texto, Cores.VERDE));
    }

    public void erro(final String texto) {
        executarNoUi(() -> linhaInterno("✗ ", texto, Cores.VERMELHO));
    }

    public void aviso(final String texto) {
        executarNoUi(() -> linhaInterno("! ", texto, Cores.AMARELO));
    }

    public void progresso(final int pct) {
        executarNoUi(() -> {
            int largura = 24;
            int cheios = Math.max(0, Math.min(largura, pct * largura / 100));
            StringBuilder barra = new StringBuilder();
            barra.append('[');
            for (int i = 0; i < largura; i++) {
                barra.append(i < cheios ? '█' : '░');
            }
            barra.append("] ").append(pct).append('%');
            linhaInterno("  ", barra.toString(), Cores.AZUL);
        });
    }

    public void sucesso(final String texto) {
        executarNoUi(() -> {
            appendInterno("\n", Cores.VERDE);
            linhaInterno("★ ", texto, Cores.VERDE);
        });
    }

    public void secao(final String titulo) {
        executarNoUi(() -> {
            appendInterno("\n", Cores.TEXTO_DIM);
            linhaInterno("── ", titulo, Cores.AZUL);
        });
    }

    public void dim(final String texto) {
        executarNoUi(() -> linhaInterno("  ", texto, Cores.TEXTO_DIM));
    }

    public void cru(final String texto) {
        if (texto == null) return;
        executarNoUi(() -> {
            appendInterno(texto, Cores.TEXTO_PRIMARIO);
            appendInterno("\n", Cores.TEXTO_PRIMARIO);
        });
    }

    public void limpar() {
        executarNoUi(() -> container.removeAllViews());
    }

    public void arvore(final java.io.File pasta,
                       final String prefixo,
                       final int profundidadeMax) {
        executarNoUi(() -> arvoreInterno(pasta, prefixo, profundidadeMax));
    }

    // ==========================================================
    //  Internos — SEMPRE no UI thread
    // ==========================================================

    private void arvoreInterno(java.io.File pasta, String prefixo, int profundidadeMax) {
        if (pasta == null || !pasta.exists()) {
            linhaInterno("✗ ", "Pasta não existe: " + pasta, Cores.VERMELHO);
            return;
        }
        if (profundidadeMax <= 0) return;

        java.io.File[] filhos = pasta.listFiles();
        if (filhos == null) return;

        java.util.Arrays.sort(filhos, (a, b) -> {
            if (a.isDirectory() && !b.isDirectory()) return -1;
            if (!a.isDirectory() && b.isDirectory()) return 1;
            return a.getName().compareToIgnoreCase(b.getName());
        });

        for (int i = 0; i < filhos.length; i++) {
            java.io.File f = filhos[i];
            boolean ultimo = (i == filhos.length - 1);
            String conector = ultimo ? "└── " : "├── ";
            String simbolo = f.isDirectory() ? "📁 " : iconeArquivo(f.getName());
            int cor = f.isDirectory() ? Cores.AZUL : Cores.TEXTO_PRIMARIO;

            linhaInterno(prefixo + conector, simbolo + f.getName(), cor);

            if (f.isDirectory()) {
                String novoPrefixo = prefixo + (ultimo ? "    " : "│   ");
                arvoreInterno(f, novoPrefixo, profundidadeMax - 1);
            }
        }
    }

    private void linhaInterno(String prefixo, String texto, int cor) {
        appendInterno(prefixo, cor);
        appendInterno(texto, cor);
        appendInterno("\n", cor);
    }

    private void appendInterno(String s, int cor) {
        SpannableStringBuilder sb = new SpannableStringBuilder();

        if (mostrarTimestamp && s.length() > 1 && !s.equals("\n")) {
            String hora = fmtHora.format(new Date());
            String prefixoHora = "[" + hora + "] ";
            int start = sb.length();
            sb.append(prefixoHora);
            sb.setSpan(new ForegroundColorSpan(Cores.TIMESTAMP),
                    start, sb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        int start = sb.length();
        sb.append(s);
        sb.setSpan(new ForegroundColorSpan(cor),
                start, sb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        TextView tv = new TextView(getContext());
        tv.setText(sb);
        tv.setTextSize(11);
        tv.setTypeface(Typeface.MONOSPACE);
        tv.setGravity(Gravity.START);
        container.addView(tv);

        post(() -> fullScroll(FOCUS_DOWN));
    }

    private void executarNoUi(Runnable r) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            r.run();
        } else {
            post(r);
        }
    }

    private String iconeArquivo(String nome) {
        if (nome.endsWith(".so")) return "🔧 ";
        if (nome.endsWith(".h") || nome.endsWith(".hpp")) return "📜 ";
        if (nome.endsWith(".cpp") || nome.endsWith(".c")) return "📄 ";
        if (nome.equals("clang") || nome.equals("clang++")) return "⚙ ";
        return "· ";
    }

    private static int dp(Context ctx, int v) {
        return (int) (v * ctx.getResources().getDisplayMetrics().density);
    }
}