package hawkins.hilos;

import hawkins.eventos.GestorEventos;
import hawkins.eventos.TipoEvento;

/**
 * Hilo encargado de generar y gestionar eventos globales de forma periódica.
 * 
 * Este generador espera un intervalo aleatorio, selecciona un evento al azar,
 * lo activa en el {@link GestorEventos} y lo desactiva cuando termina su
 * duración. Su ejecución continúa hasta que el hilo es interrumpido.
 */

public class GeneradorEventos extends Thread {

    /** Gestor central que coordina los eventos globales. */
    private GestorEventos gestorEventos;

    /** Controlador para gestionar las pausas en la simulación. */
    private ControlSimulacion controlSimulacion;

    /**
     * Crea un generador de eventos con sus dependencias.
     * 
     * @param gestorEventos     Gestor central de eventos globales.
     * @param controlSimulacion Controlador de estado para pausar o reanudar la
     *                          simulación.
     */
    public GeneradorEventos(GestorEventos gestorEventos, ControlSimulacion controlSimulacion) {
        this.gestorEventos = gestorEventos;
        this.controlSimulacion = controlSimulacion;
    }

    /**
     * Ejecuta el ciclo infinito de generación de eventos.
     * 
     * El hilo espera un tiempo aleatorio (entre 30 y 60 segundos), elige un evento
     * aleatorio de entre los disponibles, lo activa durante una duración aleatoria
     * (entre 5 y 10 segundos) y finalmente lo desactiva. Si el hilo es
     * interrumpido,
     * finaliza su ejecución de forma segura.
     */
    @Override
    public void run() {
        boolean activo = true;
        while (activo) {
            try {
                // Verificamos si la simulación se ha pausado antes de iniciar el proceso de
                // generación de eventos
                controlSimulacion.checkPausa();

                // Esperar entre 30 y 60 segundos antes del siguiente evento
                int espera = 30000 + (int) (Math.random() * 30000);
                int tiempoEsperado = 0;

                // Partimos la espera en intervalos de 100ms para poder reaccionar rápidamente a
                // una pausa
                while (tiempoEsperado < espera) {
                    controlSimulacion.checkPausa();
                    Thread.sleep(100);
                    tiempoEsperado += 100;
                }

                // Elegir evento aleatorio
                TipoEvento[] tipos = TipoEvento.values();
                TipoEvento tipo = tipos[(int) (Math.random() * tipos.length)];

                // Duración aleatoria entre 5 y 10 segundos
                int duracion = 5000 + (int) (Math.random() * 5000);

                // Verificamos si la simulación se ha pausado antes de iniciar el proceso de
                // generación de eventos
                controlSimulacion.checkPausa();

                // Activar evento
                gestorEventos.activarEvento(tipo, duracion);

                // Esperar la duración del evento
                int tiempoEvento = 0;
                while (tiempoEvento < duracion) {
                    controlSimulacion.checkPausa();
                    Thread.sleep(100);
                    tiempoEvento += 100;
                }

                // Desactivar evento
                gestorEventos.desactivarEvento();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                activo = false;
            }
        }
    }
}