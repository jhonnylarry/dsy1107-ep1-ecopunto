package cl.duoc.jolarraguibel.ecopunto.notificaciones.dominio;

public class DestinatarioInvalidoException extends RuntimeException {

    public DestinatarioInvalidoException(String destinatario) {
        super("destinatario sin correo válido: '" + destinatario + "'");
    }
}
