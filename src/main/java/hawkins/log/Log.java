package hawkins.log;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Gestor de logging seguro para escritura concurrente en archivo.
 * 
 * La clase crea y mantiene un archivo de registro dentro de la carpeta
 * txt_logs, añadiendo marcas de tiempo a cada evento escrito y garantizando
 * acceso exclusivo mediante un {@link ReentrantLock}.
 */
public class Log {

    /** Escritor con buffer para optimizar la escritura en el archivo de log. */
    private final BufferedWriter writer;

    /**
     * Cerrojo para sincronizar el acceso concurrente de múltiples hilos al archivo.
     */
    private final ReentrantLock lock;

    /** Formateador para establecer el patrón de fecha y hora del timestamp. */
    private final DateTimeFormatter formatter;

    /** Bandera que indica si el flujo del log ya ha sido cerrado. */
    private boolean cerrado;

    /**
     * Crea un log nuevo dentro de la carpeta txt_logs.
     * 
     * Si el archivo ya existía, se elimina para empezar una ejecución limpia.
     * 
     * @param archivo Nombre del archivo de log a crear o sobreescribir.
     */
    public Log(String archivo) {
        // Para no tener que poner un try-catch en el Main
        BufferedWriter tempWriter = null;
        try {
            Path carpetaLogs = Paths.get("txt_logs");
            Files.createDirectories(carpetaLogs);
            Path rutaLog = carpetaLogs.resolve(archivo);

            // En cada ejecucion se elimina el archivo previo y se crea uno nuevo.
            Files.deleteIfExists(rutaLog);
            Files.createFile(rutaLog);
            tempWriter = new BufferedWriter(new FileWriter(rutaLog.toFile(), true));
        } catch (IOException e) {
            System.err.println("Error al inicializar el log: " + e.getMessage());
        }
        // Inicializa el escritor para añadir al final del archivo (append=true)
        this.writer = tempWriter;
        this.lock = new ReentrantLock();

        // Define el formato de fecha y hora requerido
        this.formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        this.cerrado = false;
    }

    /**
     * Escribe un evento en el archivo de log con marca temporal.
     * 
     * Si el log ya está cerrado o no se pudo inicializar el escritor, no realiza
     * ninguna acción de escritura.
     * 
     * @param evento Texto descriptivo del evento a registrar en el sistema.
     */
    public void escribir(String evento) {
        lock.lock();
        try {
            if (cerrado || writer == null) {
                return;
            }

            // Obtiene fecha/hora actual y la formatea
            String fecha_actual = LocalDateTime.now().format(formatter);
            // Escribe LA HORA Y EL EVENTO A REGISTRAR
            writer.write("[" + fecha_actual + "] " + evento);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {

            System.err.println("Error al escribir en el log: " + e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    /**
     * Cierra el archivo de log de forma segura.
     * 
     * Adquiere el lock de escritura para evitar que otros hilos intenten
     * escribir mientras se cierra el flujo de datos.
     * 
     * @throws IOException Si ocurre un error de entrada/salida al cerrar el
     *                     escritor de buffer.
     */
    public void cerrar() throws IOException {
        lock.lock();
        try {
            if (!cerrado && writer != null) {
                writer.close();
                cerrado = true;
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Indica si el log ya fue cerrado.
     * 
     * @return {@code true} si el log se encuentra cerrado, {@code false} si sigue
     *         activo.
     */
    public boolean estaCerrado() {
        lock.lock();
        try {
            return cerrado;
        } finally {
            lock.unlock();
        }
    }
}