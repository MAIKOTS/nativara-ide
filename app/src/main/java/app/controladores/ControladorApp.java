package app.controladores;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.Toast;

import java.io.File;

import app.util.ModoImersivo;
import ui.navegacao.NavegadorTelas;
import ui.telas.MontadorDeEditorComAbas;
import ui.telas.MontadorDeEstrutura;
import ui.telas.MontadorDeHome;
import ui.telas.MontadorDeSeletorDeProjetos;
import ui.componentes.MenuLateralConfiguracoes; // <-- IMPORT ADICIONADO

/**
 * Orquestrador principal do app.
 */
public class ControladorApp
        implements ControladorNavegacao.Host,
                   ControladorMontadores.Host,
                   ControladorPaineis.Host,
                   MenuLateralConfiguracoes.AcoesConfig { // <-- IMPLEMENTA A INTERFACE AQUI

    private static final String URL_GITHUB = "https://github.com/MaikoTS";

    private final Activity atividade;
    private final NavegadorTelas navegador;

    private final ControladorDialogos   dialogos;
    private final ControladorMontadores montadores;
    private final ControladorNavegacao  navegacao;
    private final ControladorPaineis    paineis;

    // ==========================================================
    //  Construtor
    // ==========================================================

    public ControladorApp(Activity atividade) {
        this.atividade = atividade;
        this.navegador = new NavegadorTelas(atividade);

        // 1. Sub-controladores
        this.montadores = new ControladorMontadores(atividade, navegador, this);
        this.navegacao  = new ControladorNavegacao (atividade, navegador, this);
        
        // O ControladorPaineis agora vai precisar receber "this" para saber quem 
        // implementa as ações do menu de configurações (mais abaixo explicamos isso)
        this.paineis    = new ControladorPaineis    (atividade, navegador, this);

        // 2. Dialogos (só notificam)
        this.dialogos = new ControladorDialogos(new ControladorDialogos.Acoes() {
            @Override
            public void aoAbrirProjetoNaEstrutura(File pasta, String rotulo) {
                navegacao.abrirProjetoNaEstrutura(pasta, rotulo);
            }

            @Override
            public void aoEditarArquivoNoEditor(File arquivo) {
                montadores.editor().abrirArquivo(arquivo);
                navegador.navegarPara(NavegadorTelas.EstadoTela.TELA_EDITOR);
                ModoImersivo.aplicar(atividade);
            }

            @Override
            public void aoAbrirExploradorFiles() {
                navegacao.abrirExploradorFiles();
            }

            @Override
            public void aoAbrirFerramentasIde() {
                navegacao.abrirFerramentasIde();
            }

            @Override
            public void aoAbrirGerenciadorProjetos() {
                navegacao.abrirGerenciadorProjetos();
            }

            @Override
            public void aoAbrirClonarRepositorio() {
                navegacao.abrirClonarRepositorio();
            }
        });

        // 3. Ordem de inicialização
        montadores.inicializar();
        paineis.inicializar();
        montadores.registrarViewsIniciais();

        // 4. Listener de troca de tela (pra habilitar/desabilitar menu direito)
        navegador.setOnTelaMudouListener(this::aoMudarTela);

        // 5. Tela inicial
        navegador.navegarPara(NavegadorTelas.EstadoTela.TELA_INICIAL);
    }

    public View construirViewPrincipal() {
        return paineis.obterPainelRaiz();
    }

    // ==========================================================
    //  Ciclo de vida
    // ==========================================================

    public boolean aoVoltar() {
        if (paineis.isDireitoAberto()) {
            paineis.fecharDireito();
            ModoImersivo.aplicar(atividade);
            return true;
        }

        if (paineis.isEsquerdoAberto()) {
            paineis.fecharEsquerdo();
            ModoImersivo.aplicar(atividade);
            return true;
        }

        if (navegador.obterTelaAtual() == NavegadorTelas.EstadoTela.TELA_INICIAL) {
            return false;
        }

        navegador.voltarTelaAnterior();

        if (navegador.obterTelaAtual() == NavegadorTelas.EstadoTela.TELA_INICIAL) {
            montadores.home().atualizarRecentes();
        }
        ModoImersivo.aplicar(atividade);
        return true;
    }

    public void aoGanharFoco() {
        ModoImersivo.aplicar(atividade);
    }

    private void aoMudarTela(NavegadorTelas.EstadoTela novaTela) {
        boolean ehHome = (novaTela == NavegadorTelas.EstadoTela.TELA_INICIAL);
        paineis.setHabilitadoDireito(ehHome);
    }

    // ==========================================================
    //  Host: ControladorMontadores e ControladorNavegacao e Painéis
    // ==========================================================

    @Override
    public void aoAbrirMenuEsquerdo() {
        paineis.abrirEsquerdo();
    }

    @Override
    public void aoAbrirMenuDireito() {
        if (navegador.obterTelaAtual() != NavegadorTelas.EstadoTela.TELA_INICIAL) {
            return;
        }
        paineis.abrirDireito();
    }

    @Override
    public void aoCliqueCriarNovoProjeto() {
        navegacao.abrirNovoProjeto();
    }

    @Override
    public void aoAbrirGerenciadorProjetos() {
        navegacao.abrirGerenciadorProjetos();
    }

    @Override
    public void aoAbrirClonarRepositorio() {
        navegacao.abrirClonarRepositorio();
    }

    @Override
    public void aoImportarProjeto() {
        navegador.navegarPara(NavegadorTelas.EstadoTela.TELA_SELETOR_PROJETO);
    }

    @Override
    public void aoAbrirProjetoRecente(File pasta) {
        if (pasta == null || !pasta.exists()) {
            toast("Projeto não encontrado.");
            return;
        }
        navegacao.abrirProjetoNaEstrutura(pasta, "Projeto carregado");
    }

    @Override
    public void aoFecharEditor() {
        if (!navegador.voltarTelaAnterior()) {
            navegador.navegarPara(NavegadorTelas.EstadoTela.TELA_INICIAL);
        }
        ModoImersivo.aplicar(atividade);
    }

    @Override
    public void aoAbrirConfigsProjeto(File pastaProjeto) {
        navegacao.abrirConfigProjeto(pastaProjeto);
    }

    @Override
    public void aoSelecionarArquivo(File arquivo) {
        montadores.editor().abrirArquivo(arquivo);
        navegador.navegarPara(NavegadorTelas.EstadoTela.TELA_EDITOR);
        ModoImersivo.aplicar(atividade);
    }

    @Override
    public void aoVoltarDaEstrutura() {
        if (!navegador.voltarTelaAnterior()) {
            navegador.navegarPara(NavegadorTelas.EstadoTela.TELA_INICIAL);
        }
        ModoImersivo.aplicar(atividade);
    }

    @Override
    public void aoNavegarPeloMenu(NavegadorTelas.EstadoTela tela) {
        if (tela != NavegadorTelas.EstadoTela.TELA_INICIAL) {
            navegador.navegarPara(NavegadorTelas.EstadoTela.TELA_INICIAL);
        } else {
            montadores.home().atualizarRecentes();
        }
        navegador.navegarPara(tela);
        paineis.fecharEsquerdo();
    }

    @Override
    public void aoAbrirFerramentasIdePeloMenu() {
        navegacao.abrirFerramentasIde();
    }

    @Override
    public void aoAlterarTamanhoFonte(int novoTamanho) {
        toast("Fonte: " + novoTamanho + "sp");
    }

    @Override public void aoAlternarWordWrap(boolean ativo)         { }
    @Override public void aoAlternarNumerosLinha(boolean ativo)     { }
    @Override public void aoAlternarConsoleAuto(boolean ativo)      { }
    @Override public void aoAlternarSalvarAoSair(boolean ativo)     { }

    @Override
    public void aoClicarSobre() {
        mostrarDialogoSobre();
    }

    @Override
    public void aoClicarGithub() {
        abrirUrl(URL_GITHUB);
    }

    @Override
    public void aoClicarReportar() {
        abrirUrl(URL_GITHUB + "/issues");
    }

    @Override
    public MontadorDeHome montadorHome() { return montadores.home(); }
    @Override
    public MontadorDeSeletorDeProjetos montadorSeletor() { return montadores.seletor(); }
    @Override
    public MontadorDeEstrutura montadorEstrutura() { return montadores.estrutura(); }
    @Override
    public MontadorDeEditorComAbas montadorEditor() { return montadores.editor(); }

    @Override
    public void toast(String msg) {
        Toast.makeText(atividade, msg, Toast.LENGTH_SHORT).show();
    }

    // ==========================================================
    // ★ NOVO: Implementação da Interface AcoesConfig
    // ==========================================================

    @Override
    public void aoFechar() {
        paineis.fecharDireito();
        ModoImersivo.aplicar(atividade);
    }

    @Override
    public void aoClicarNotificacoes() {
        // Aqui você pode abrir uma nova tela ou mostrar um Dialogo
        toast("Notificações: Em breve!");
    }

    @Override
    public void aoClicarVerificarAtualizacoes() {
        // Aqui no futuro você coloca a lógica para checar a API do GitHub
        toast("Verificando atualizações...");
    }

    @Override
    public void aoClicarInformacoes() {
        // Reutiliza a sua janela de "Sobre"
        mostrarDialogoSobre();
    }

    // ==========================================================
    //  Utilitários
    // ==========================================================

    private void mostrarDialogoSobre() {
        // ATUALIZADO COM O NOVO NOME
        ui.dialogos.DialogoApp.confirmar(atividade,
                "Nativara IDE",
                "Versão: 1.00\n\n" +
                "IDE mobile para desenvolvimento C/C++ nativo no Android.\n\n" +
                "Compilador Clang 14 • NDK r24\n" +
                "Suporte multi-ABI (ARM64 + ARM32)\n\n" +
                "Desenvolvido por MaikoTS",
                "OK", null, null);
    }

    private void abrirUrl(String url) {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            atividade.startActivity(i);
        } catch (Exception e) {
            toast("Não foi possível abrir o navegador.");
        }
    }
}