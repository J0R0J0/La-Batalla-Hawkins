package hawkins.hilos;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Clase responsable de controlar el estado de pausa y reanudación de la
 * simulación.
 * Emplea mecanismos de concurrencia mediante Locks para garantizar la
 * sincronización segura entre los distintos hilos.
 */
public class ControlSimulacion {

    /**
     * Estado actual de la simulación.
     * volatile asegura que los cambios sean visibles de forma inmediata entre todos
     * los hilos.
     */
    private volatile boolean paused = false;

    /**
     * Lock principal utilizado para proteger el acceso concurrente al estado
     * 'paused' y a su condición asociada.
     */
    private final ReentrantLock pauseLock = new ReentrantLock();

    /**
     * Condición asociada al lock, empleada para que los hilos esperen
     * eficientemente
     * y sin consumir CPU hasta que la variable 'paused' sea falsa.
     */
    private final Condition unpaused = pauseLock.newCondition();

    /**
     * Constructor por defecto de la clase ControlSimulacion.
     */
    public ControlSimulacion() {
    }

    /**
     * Marca la simulación como pausada.
     * Adquiere el lock internamente para asegurar que la operación de cambio de
     * estado sea completamente atómica.
     */
    public void pausar() {
        // Adquirimos el cerrojo para la modificación del estado de pausa
        pauseLock.lock();
        try {
            if (!paused) {
                paused = true;
                System.out.println("---- SIMULACION PAUSADA ----");
            }
        } finally {
            pauseLock.unlock();
        }
    }

    /**
     * Marca la simulación como no pausada (reanudada).
     * Despierta a todos los hilos que estuvieran esperando en la Condition.
     * Adquiere el lock para asegurar que la operación es atómica y está
     * correctamente coordinada con
     * las llamadas a {@link Condition#await()}.
     */
    public void reanudar() {
        // Adquirimos el cerrojo para la modificación del estado de pausa y para
        // despertar a los hilos
        pauseLock.lock();
        try {
            if (paused) {
                paused = false;
                unpaused.signalAll();
                System.out.println("---- SIMULACION REANUDADA ----");
            }
        } finally {
            pauseLock.unlock();
        }
    }

    /**
     * Método que deben invocar los hilos (Humano, Zombi, etc.) periódicamente
     * dentro de sus respectivos bucles 'run()' para comprobar si deben pausar su
     * ejecución.
     * Si la simulación se encuentra pausada, el hilo que realice la llamada quedará
     * bloqueado
     * en este punto de forma eficiente hasta que se ejecute el método
     * {@link #reanudar()}.
     *
     * @throws InterruptedException Si el hilo en ejecución es interrumpido mientras
     *                              se encuentra esperando en
     *                              estado await().
     */
    public void checkPausa() throws InterruptedException {
        // Al ser una un atributo volatile, la lectura de la variable es atómica
        // y no requiere adquirir el lock si no está pausado.
        if (!paused) {
            return;
        }

        // En caso de que la simulación esté pausada, adquirimos el cerrojo
        // y hacemos que el hilo se duerma de forma eficiente hasta que se vuelva a
        // reanudar la simulación. Esto evita la espera activa.
        pauseLock.lock();
        try {
            // Se duerme el hilo
            while (paused) {
                unpaused.await();
            }

        } finally {
            pauseLock.unlock();
        }
    }

    /**
     * Devuelve el estado actual de la pausa (especialmente útil para consultas
     * desde la GUI o RMI).
     * Dado que la lectura de un booleano con el modificador volatile es atómica,
     * este método
     * no requiere adquirir un lock para su consulta.
     * 
     * @return {@code true} si la simulación está actualmente pausada, {@code false}
     *         en caso contrario.
     */
    public boolean isPausada() {
        return paused;
    }
}