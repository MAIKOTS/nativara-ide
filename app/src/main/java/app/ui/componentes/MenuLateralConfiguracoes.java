package ui.componentes;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import app.R;

/**
 * Segundo menu lateral — CONFIGURAÇÕES.
 * Abre pela DIREITA. Estende PainelLateralDireita.
 */
public class MenuLateralConfiguracoes extends PainelLateralDireita {

    // Interface expandida para receber os novos cliques
    public interface AcoesConfig {
        void aoFechar();
        void aoClicarNotificacoes();
        void aoClicarVerificarAtualizacoes();
        void aoClicarInformacoes();
    }

    public MenuLateralConfiguracoes(Context ctx,
                                    View conteudoPrincipal,
                                    final AcoesConfig acoes) {
        super(ctx, conteudoPrincipal, inflarMenu(ctx));

        LinearLayout menu = obterMenuLateral();
        if (menu != null && acoes != null) {

            // Opção: Notificações
            View opcNotificacoes = menu.findViewById(R.id.opcNotificacoes);
            if (opcNotificacoes != null) {
                opcNotificacoes.setOnClickListener(v -> acoes.aoClicarNotificacoes());
            }

            // Opção: Verificar Atualizações
            View opcVerificarAtualizacoes = menu.findViewById(R.id.opcVerificarAtualizacoes);
            if (opcVerificarAtualizacoes != null) {
                opcVerificarAtualizacoes.setOnClickListener(v -> acoes.aoClicarVerificarAtualizacoes());
            }

            // Opção: Informações da IDE
            View opcInformacoes = menu.findViewById(R.id.opcInformacoes);
            if (opcInformacoes != null) {
                opcInformacoes.setOnClickListener(v -> acoes.aoClicarInformacoes());
            }
        }
    }

    private static LinearLayout inflarMenu(Context ctx) {
        return (LinearLayout) LayoutInflater.from(ctx)
                .inflate(R.layout.view_configuracoes, null, false);
    }
}