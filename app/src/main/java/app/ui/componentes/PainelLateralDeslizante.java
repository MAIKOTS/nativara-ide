package ui.componentes;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import ui.util.Cores;

public class PainelLateralDeslizante extends FrameLayout {

    private final View conteudoPrincipal;
    private final LinearLayout menuLateral;
    private final View sombraFundo;
    private boolean menuAberto = false;
    private float pontoToqueInicialX = 0;

    private final int larguraMenu;
    private final float margemBordaPx;

    public PainelLateralDeslizante(Context contexto, View conteudoPrincipal, LinearLayout menuLateral) {
        super(contexto);
        this.conteudoPrincipal = conteudoPrincipal;
        this.menuLateral = menuLateral;

        margemBordaPx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 30, contexto.getResources().getDisplayMetrics());

        int larguraTela = contexto.getResources().getDisplayMetrics().widthPixels;
        this.larguraMenu = (int) (larguraTela * 0.50f);

        setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        // 1. Conteúdo Principal
        addView(conteudoPrincipal);

        // 2. Sombra / Overlay
        sombraFundo = new View(contexto);
        sombraFundo.setBackgroundColor(Cores.OVERLAY);
        sombraFundo.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        sombraFundo.setVisibility(View.GONE);

        sombraFundo.setOnTouchListener((v, event) -> {
            if (menuAberto && event.getAction() == MotionEvent.ACTION_UP) {
                fecharMenu();
            }
            return true;
        });
        addView(sombraFundo);

        // 3. Estilo do Menu Lateral
        GradientDrawable drawableFundo = new GradientDrawable();
        drawableFundo.setColor(Cores.FUNDO_MENU);

        float raio = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 4, contexto.getResources().getDisplayMetrics());
        drawableFundo.setCornerRadii(new float[]{0, 0, raio, raio, raio, raio, 0, 0});

        int larguraBordaPx = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 1, contexto.getResources().getDisplayMetrics());
        drawableFundo.setStroke(larguraBordaPx, Cores.BORDA);

        menuLateral.setBackground(drawableFundo);

        FrameLayout.LayoutParams paramsMenu = new FrameLayout.LayoutParams(
                larguraMenu,
                ViewGroup.LayoutParams.MATCH_PARENT,
                Gravity.START);
        menuLateral.setLayoutParams(paramsMenu);
        menuLateral.setTranslationX(-larguraMenu);

        menuLateral.setClickable(true);
        menuLateral.setFocusable(true);

        addView(menuLateral);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        float x = ev.getX();

        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                pontoToqueInicialX = x;
                if (menuAberto || x <= margemBordaPx) {
                    return false;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                float deltaX = x - pontoToqueInicialX;
                if (!menuAberto && pontoToqueInicialX <= margemBordaPx && deltaX > 10) {
                    return true;
                }
                if (menuAberto && deltaX < -10) {
                    return true;
                }
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent evento) {
        float x = evento.getX();

        switch (evento.getAction()) {
            case MotionEvent.ACTION_DOWN:
                pontoToqueInicialX = x;
                return menuAberto || pontoToqueInicialX <= margemBordaPx;

            case MotionEvent.ACTION_MOVE:
                if (!menuAberto && pontoToqueInicialX > margemBordaPx) return false;

                float deslocamentoX = x - pontoToqueInicialX;
                if (menuAberto) {
                    if (deslocamentoX < 0) {
                        float novaPosicao = Math.max(-larguraMenu, deslocamentoX);
                        menuLateral.setTranslationX(novaPosicao);

                        float progresso = (larguraMenu + novaPosicao) / (float) larguraMenu;
                        sombraFundo.setAlpha(progresso);
                    }
                } else {
                    float novaPosicao = Math.min(0, -larguraMenu + deslocamentoX);
                    menuLateral.setTranslationX(novaPosicao);
                    sombraFundo.setVisibility(View.VISIBLE);

                    float progresso = (larguraMenu + novaPosicao) / (float) larguraMenu;
                    sombraFundo.setAlpha(progresso);
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (!menuAberto && pontoToqueInicialX > margemBordaPx) return false;

                float posicaoAtual = menuLateral.getTranslationX();
                if (posicaoAtual > -((float) larguraMenu / 2)) {
                    abrirMenu();
                } else {
                    fecharMenu();
                }
                return true;
        }
        return super.onTouchEvent(evento);
    }

    public void abrirMenu() {
        sombraFundo.setVisibility(View.VISIBLE);
        sombraFundo.animate().alpha(1.0f).setDuration(200).start();
        menuLateral.animate().translationX(0).setDuration(200).start();
        menuAberto = true;
    }

    public void fecharMenu() {
        sombraFundo.animate().alpha(0.0f).setDuration(200).withEndAction(() -> {
            sombraFundo.setVisibility(View.GONE);
        }).start();

        menuLateral.animate().translationX(-larguraMenu).setDuration(200).start();
        menuAberto = false;
    }

    public boolean isMenuAberto() {
        return menuAberto;
    }
}