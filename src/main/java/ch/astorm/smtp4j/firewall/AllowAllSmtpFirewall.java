package ch.astorm.smtp4j.firewall;

public final class AllowAllSmtpFirewall implements SmtpFirewall {
    public static final AllowAllSmtpFirewall INSTANCE = new AllowAllSmtpFirewall();

    private AllowAllSmtpFirewall() {
    }
}
