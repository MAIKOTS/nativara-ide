package ui.util;

/**
 * Paleta centralizada do app.
 *
 * Uso:
 *   textView.setTextColor(Cores.TEXTO_PRIMARIO);
 *   chip.setTextColor(Cores.VERDE);
 */
public final class Cores {

    // ---------- Superfícies ----------
    public static final int FUNDO_RAIZ       = 0xFF0A0A0C;
    public static final int FUNDO_CARD       = 0xFF0F0F12;
    public static final int FUNDO_CARD_ALT   = 0xFF0C0C0E;
    public static final int FUNDO_MENU       = 0xFF18181B;
    public static final int BORDA            = 0xFF27272A;
    public static final int OVERLAY          = 0x99000000;

    // ---------- Texto ----------
    public static final int TEXTO_PRIMARIO   = 0xFFF4F4F5;
    public static final int TEXTO            = 0xFFE4E4E7;
    public static final int TEXTO_SECUNDARIO = 0xFFA1A1AA;
    public static final int TEXTO_DIM        = 0xFF6B7280;
    public static final int TEXTO_HINT       = 0xFF52525B;
    public static final int TEXTO_FANTASMA   = 0xFF3F3F46;

    // ---------- Acentos ----------
    public static final int VERDE            = 0xFF00E676;
    public static final int VERDE_FUNDO      = 0xFF0D2A19;
    public static final int VERMELHO         = 0xFFFF5252;
    public static final int AMARELO          = 0xFFFFC107;
    public static final int AZUL             = 0xFF60A5FA;
    public static final int ROXO             = 0xFFA78BFA;
    public static final int LARANJA          = 0xFFF59E0B;
    public static final int DOURADO          = 0xFFFBBF24;

    // ---------- Editor / Console ----------
    public static final int DESTAQUE_LINHA   = 0x10FFFFFF;  // overlay sutil branco
    public static final int TIMESTAMP        = 0xFF52525B;
    public static final int PROMPT           = 0xFF00E676;

    private Cores() { }
}