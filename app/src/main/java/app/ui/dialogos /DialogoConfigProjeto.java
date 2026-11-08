package ui.dialogos;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;

public class DialogoConfigProjeto extends Dialog {

    public DialogoConfigProjeto(Context context, View conteudo) {
        // Usa o tema translúcido do sistema para não ter barras brancas
        super(context, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
        
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(conteudo);
        setCancelable(true);
        setCanceledOnTouchOutside(true); // Fecha ao clicar fora do card

        Window window = getWindow();
        if (window != null) {
            // Fundo transparente para o scrim desenhado no XML aparecer
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        aplicarModoImersivoNaJanela();
    }

    private void aplicarModoImersivoNaJanela() {
        Window window = getWindow();
        if (window == null) return;

        // 1. Desenhar atrás do Notch
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams params = window.getAttributes();
            params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            window.setAttributes(params);
        }

        // 2. Esconder as barras do sistema na janela do Dialog
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }
}