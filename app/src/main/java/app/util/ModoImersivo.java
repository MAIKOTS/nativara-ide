package app.util;

import android.app.Activity;
import android.os.Build;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;

public final class ModoImersivo {

    private ModoImersivo() { }

    public static void aplicar(Activity atividade) {
        if (atividade == null) return;

        // 1. Desenhar atrás do Notch / Furo da câmera (Android 9+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams params = atividade.getWindow().getAttributes();
            params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            atividade.getWindow().setAttributes(params);
        }

        // 2. Esconder barras de navegação e status (Android 11+ / API 30+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = atividade.getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                // Permite que as barras apareçam temporariamente (transparentes) ao deslizar na borda
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } 
        // 3. Fallback para versões antigas do Android
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            aplicarModoAntigo(atividade.getWindow().getDecorView());
        }
    }

    @SuppressWarnings("deprecation")
    private static void aplicarModoAntigo(View decorView) {
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }
}