package ui.componentes;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.Layout;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.EditText;

import ui.util.Cores;

/**
 * Coluna de numeração de linhas FIXA à esquerda do editor.
 */
public class GutterView extends View {

    private EditText editor;
    private ScrollView2D scrollView;
    private ViewTreeObserver.OnScrollChangedListener scrollListener;

    private final Paint paintNumero   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintDestaque = new Paint();

    private int larguraMinima;
    private int larguraGutter;

    public GutterView(Context context) {
        this(context, null);
    }

    public GutterView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context ctx) {
        paintNumero.setColor(Cores.TEXTO_HINT);
        paintNumero.setTextSize(sp(ctx, 12));
        paintNumero.setTypeface(Typeface.MONOSPACE);
        paintNumero.setTextAlign(Paint.Align.RIGHT);

        paintDestaque.setColor(Cores.DESTAQUE_LINHA);

        larguraMinima = (int) dp(ctx, 20);
        larguraGutter = larguraMinima;
    }

    public void setEditor(EditText e) {
        this.editor = e;
        atualizarLargura();
        invalidate();
    }

    public void setScrollView(ScrollView2D sv) {
        if (scrollView != null && scrollListener != null) {
            scrollView.getViewTreeObserver().removeOnScrollChangedListener(scrollListener);
        }

        this.scrollView = sv;

        if (sv != null) {
            scrollListener = this::invalidate;
            sv.getViewTreeObserver().addOnScrollChangedListener(scrollListener);
        }
    }

    private void atualizarLargura() {
        if (editor == null) return;
        Layout layout = editor.getLayout();
        if (layout == null) return;

        int totalLinhas = Math.max(layout.getLineCount(), 1);
        String maior = String.valueOf(totalLinhas);

        int larguraCalculada = (int) (paintNumero.measureText(maior)
                + dp(getContext(), 6)
                + dp(getContext(), 8));

        if (larguraCalculada != larguraGutter) {
            larguraGutter = Math.max(larguraCalculada, larguraMinima);
            requestLayout();
        }
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        atualizarLargura();
        int width = resolveSize(larguraGutter, widthSpec);
        int height = resolveSize(getSuggestedMinimumHeight(), heightSpec);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // 1. Fundo
        canvas.drawColor(Cores.FUNDO_RAIZ);

        if (editor == null || scrollView == null) return;

        Layout layout = editor.getLayout();
        if (layout == null) return;

        int scrollY = scrollView.getScrollY();
        int padTop = editor.getPaddingTop();
        int linhaCursor = layout.getLineForOffset(editor.getSelectionStart());
        int totalLinhas = layout.getLineCount();
        int alturaView = getHeight();

        // 2. Destaque da linha atual
        int yTop = layout.getLineTop(linhaCursor) + padTop - scrollY;
        int yBottom = layout.getLineBottom(linhaCursor) + padTop - scrollY;
        if (yBottom > 0 && yTop < alturaView) {
            canvas.drawRect(0, yTop, getWidth(), yBottom, paintDestaque);
        }

        // 3. Números (com culling)
        float xNumero = getWidth() - dp(getContext(), 6);

        for (int i = 0; i < totalLinhas; i++) {
            int base = layout.getLineBaseline(i) + padTop - scrollY;

            if (base < -50 || base > alturaView + 50) continue;

            int cor = (i == linhaCursor) ? Cores.VERDE : Cores.TEXTO_HINT;
            paintNumero.setColor(cor);
            canvas.drawText(String.valueOf(i + 1), xNumero, base, paintNumero);
        }
    }

    private float dp(Context ctx, int v) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, v,
                ctx.getResources().getDisplayMetrics());
    }

    private float sp(Context ctx, int v) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, v,
                ctx.getResources().getDisplayMetrics());
    }
}