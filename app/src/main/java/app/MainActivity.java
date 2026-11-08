package app;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;

import app.controladores.ControladorApp;
import app.util.LibsNativas;
import app.util.ModoImersivo;
import app.util.Permissoes;
import globalclass.settings.AppGlobalSettings;
import globalclass.settings.LogApp;

public class MainActivity extends Activity {

    static {
        LibsNativas.carregar();
    }

    private ControladorApp controlador;

    @Override
    protected void onCreate(Bundle estadoInstancia) {
        super.onCreate(estadoInstancia);

        AppGlobalSettings.inicializar(this);

        // Permite desenhar atrás das barras no Android 11+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
        }

        ModoImersivo.aplicar(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        LogApp.inicializar(this);
        LogApp.i("Nativara IDE inicializado com sucesso!");

        Permissoes.solicitarArmazenamento(this);

        controlador = new ControladorApp(this);
        setContentView(controlador.construirViewPrincipal());
    }

    @Override
    public void onBackPressed() {
        if (!controlador.aoVoltar()) {
            super.onBackPressed();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean temFoco) {
        super.onWindowFocusChanged(temFoco);
        if (temFoco) {
            // Sempre que o app ganha foco (fecha teclado, fecha dialogo, volta de outro app),
            // reaplicamos o modo imersivo.
            controlador.aoGanharFoco(); 
        }
    }
}