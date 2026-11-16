package hawkins.hilos;

import hawkins.eventos.GestorEventos;
import hawkins.log.Log;
import hawkins.zonas.upsidedown.Colmena;
import hawkins.zonas.upsidedown.ZonaUpsideDown;

/**
 * Hilo que representa a un Demogorgon en el Upside Down.
 * 
 * El Demogorgon se desplaza entre zonas, busca niños para atacarlos, los
 * captura si consigue vencerles y los deposita en la Colmena. Su comportamiento
 * también reacciona a los eventos globales como tormentas, apagones, Eleven y
 * la Red Mental.
 */
public class Demogorgon extends Thread {

    /** Identificador numérico del Demogorgon. */
    private int id;

    /** Nombre legible del Demogorgon. */
    private String nombre;

    /** Zonas disponibles del Upside Down. */
    private ZonaUpsideDown[] zonas;

    /** Zona actual donde se encuentra el Demogorgon. */
    private ZonaUpsideDown zonaActual;

    /** Referencia a la Colmena donde se depositan los niños capturados. */
    private Colmena colmena;

    /** Gestor de eventos globales para consultar estados especiales. */
    private GestorEventos gestorEventos;

    /** Logger para registrar la actividad del Demogorgon. */
    private Log logger;

    /** Número total de capturas realizadas por este Demogorgon. */
    private int contCapturas;

    /** Controlador para la pausa en las simulaciones. */
    private ControlSimulacion controlSimulacion;

    /**
     * Crea un Demogorgon con su identificador y dependencias necesarias.
     * 
     * @param id            Identificador numérico del Demogorgon
     * @param zonas         Zonas disponibles del Upside Down
     * @param colmena       Colmena donde depositará los niños capturados
     * @param gestorEventos Gestor de eventos globales
     * @param logger        Logger para registrar su actividad
     */
    public Demogorgon(int id, ZonaUpsideDown[] zonas, Colmena colmena, GestorEventos gestorEventos, Log logger,
            ControlSimulacion controlSimulacion) {
        // Atributos de identificación (Id y Nombre)
        this.id = id;
        this.nombre = String.format("D%04d", this.id);

        // Referencia a las zonas del Upside Down
        this.zonas = zonas;

        // el Demogorgon aún no ha entrado en ninguna zona
        this.zonaActual = null;
        this.colmena = colmena;
        this.gestorEventos = gestorEventos;

        // Logger para imprimir los eventos del Demogorgon
        this.logger = logger;

        // Contador de capturas del Demogorgon
        this.contCapturas = 0;
        this.controlSimulacion = controlSimulacion;
    }

    /*
     * Ciclo de Vida de los Demogorgones:
     * 1. Nada más nacer, los Demogorgones seleccionarán aleatoriamente
     * una de las 4 zonas inseguras del UpsideDown (BOSQUE, LABORATORIO,
     * CENTRO COMPERCIAL Y ALCANTARILLADO).
     * 2. Una vez en el área seleccionado, comprobará si hay algún niño en
     * esa zona. Si hay almenos un niño (seleccionará uno aleatoriamente),
     * el Demogorgor intentará capturarlo (esta acción tiene una duracción
     * aleatorio entr 0,5 y 1,5 s)
     * 2.1. En caso de que el ataque sea exitoso, el niño se trasdalará a la
     * COLMENA y una vez dentro de la COLMENA (se tardará 0,5 y 1 s) se aumentará
     * el contador de capturas del DEMOGORGON en 1.
     * 2.2. En caso de que el ataque no sea exitoso, el niño se mantendrá en el
     * área.
     * 3. En cualquiera de los dos casos, el Demogorgon, tras el ataque,
     * se desplazará a otra zona aleatoriamente.
     * 4. En caso de que el Demogorgon entra a un área sin niños lo que hará
     * es mantenerse en esa zona un tiempo aleatorio entre 4 y 5 s antes de volver
     * a desplazarse a otra zona aleatoriamente sin poder repetir la zona donde
     * acaba de estar.
     * 5. Por cada 8 niños capturados en la COLMENA, Vecna generará un nuevo
     * Demogorgon
     * 6. Este ciclo se repite indefinidamente
     */

    @Override
    public void run() {
        // Elegir zona inicial aleatoriamente
        zonaActual = zonas[(int) (Math.random() * zonas.length)];

        // El demogorgon entra en la zona inicial que ha elegido aleatoriamente
        zonaActual.entrarDemogorgon(this);
        logger.escribir("El demogorgon " + nombre + " aparece en " + zonaActual.getNombre());

        boolean activo = true;

        while (activo) {
            try {
                // Comprobamos la pausa de la simulacion
                controlSimulacion.checkPausa();

                // Comprobamos si está paralizado por Eleven
                gestorEventos.esperarSiParalizado();

                // Selecionamos a un niño aleatorio de la zona donde se encuentre el
                // Demogorgon, y lo intentamos capturar
                Nino objetivo = zonaActual.tomarNinoParaAtaque();

                if (objetivo != null) {
                    // Elegir un niño aleatorio para atacar
                    logger.escribir("El demogorgon " + nombre + " ataca al niño " +
                            objetivo.getNombre() + " en " + zonaActual.getNombre() +
                            " (capturas: " + contCapturas + ")");

                    // Atacar al niño (duración aleatoria entre 0.5 y 1.5s)
                    int duracionAtaque = 500 + (int) (Math.random() * 1000);

                    // Tormenta: tiempo del ataque se reduce a la mitad
                    if (gestorEventos.isTormentaActiva()) {
                        duracionAtaque /= 2;
                        logger.escribir("El demogorgon " + nombre + " [TORMENTA] ataca más rápido" +
                                ", la duración del ataque se reduce a " + duracionAtaque + " ms" +
                                " de lo normal (" + duracionAtaque * 2 + " ms)");
                    }

                    // Se intenta capturar al niño (el método serAtacado devuelve true si el niño es
                    // capturado)
                    boolean capturado = objetivo.serAtacado(duracionAtaque);

                    // Verificamos si la simulación se ha pasausado antes de procesar el resultado
                    // del ataque
                    controlSimulacion.checkPausa();

                    if (capturado) {
                        // Depositar al niño en la Colmena (tarda entre 0.5 y 1s)
                        int tiempoDeposito = 500 + (int) (Math.random() * 500);
                        Thread.sleep(tiempoDeposito);
                        colmena.depositarNino(objetivo);
                        contCapturas++;

                        logger.escribir("El demogorgon " + nombre + " captura al niño " +
                                objetivo.getNombre() + " (capturas: " + contCapturas + ")");

                        // Verificamos si la simulación se ha pasausado antes de continuar con el
                        // siguiente ciclo
                        controlSimulacion.checkPausa();
                    } else {
                        // Devolvemos al niño en caso de que haya resistido el ataque
                        zonaActual.devolverNino(objetivo);
                        logger.escribir("El niño " + objetivo.getNombre() +
                                " resistió el ataque del demogorgon " + nombre);
                    }
                } else {
                    // No hay niños, esperar entre 4 y 5 segundos
                    logger.escribir("El demogorgon " + nombre + " no encuentra niños en " +
                            zonaActual.getNombre() + ", esperando...");
                    int espera = 4000 + (int) (Math.random() * 1000);
                    int tiempoTranscurrido = 0;

                    // Realizamos este bucle para pausar al Demogorgon de forma que si se pausa la
                    // simulación
                    // el Demogorgon se pause, ya que el método sleep() no se puede interrumpir de
                    // ninguna manera.
                    // Entonces al hacerlo en cortos peridos de tiempo, conseguimos que el
                    // Demogorgon se "despierte",
                    // compruebe si la simulación se ha pausado, y si es así, se quede bloqueado en
                    // el método checkPausa()
                    // hasta que se reanude la simulación.
                    while (tiempoTranscurrido < espera) {
                        // Verificamos periódicamente si la simulación se ha pausado durante la espera
                        controlSimulacion.checkPausa();

                        // Si Eleven se activa durante la espera, nos bloqueamos aquí
                        gestorEventos.esperarSiParalizado();
                        Thread.sleep(100);
                        tiempoTranscurrido += 100;
                    }
                }

                // Verificamos si la simulación se ha pausado antes de intentar desplazarnos a
                // otra zona
                controlSimulacion.checkPausa();

                // Si hay eventos activos, el Demogorgon dependiendo del evento se entrá que
                // esperar
                // o no, así que por eso la línea es esta:
                gestorEventos.esperarSiParalizado();

                // Verificamos si la simulación se ha pausado antes de intentar desplazarnos a
                // otra zona
                controlSimulacion.checkPausa();

                // Desplazarse a otra zona aleatoriamente
                zonaActual.salirDemogorgon(this);

                // Verificamos si hay eventos globales que afecten el comportamiento del
                // Demogorgon, si
                // no lo hay, entra a la siguiente zona y repita el ciclo con normalidad
                if (gestorEventos.isApagonActivo()) {
                    // Apagón: no se puede mover el demogorgon de la zona actual
                    zonaActual.entrarDemogorgon(this);
                    logger.escribir("El demogorgon " + nombre + " [APAGON] no puede moverse y se queda en " +
                            zonaActual.getNombre());
                } else {
                    if (gestorEventos.isRedMentalActiva()) {
                        // Red Mental: ir a la zona con más niños
                        zonaActual = gestorEventos.getZonaConMasNinos();

                        // Verificamos si la simulación se ha pausado antes de intentar desplazarnos a
                        // otra zona
                        controlSimulacion.checkPausa();

                        // El Demogorgon se desplaza a la zona con más niños
                        zonaActual.entrarDemogorgon(this);
                        logger.escribir("El demogorgon " + nombre +
                                " [RED MENTAL] se dirige a " + zonaActual.getNombre());
                    } else {
                        ZonaUpsideDown siguienteZona = elegirSiguienteZona();
                        zonaActual = siguienteZona;

                        // Verificamos si la simulación se ha pausado antes de intentar desplazarnos a
                        // otra zona
                        controlSimulacion.checkPausa();

                        // El Demogorgon se desplaza a la siguiente zona
                        zonaActual.entrarDemogorgon(this);
                        logger.escribir("El demogorgon " + nombre + " se desplaza a " +
                                zonaActual.getNombre());
                    }
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                activo = false;
            }
        }
    }

    /**
     * Elige una zona aleatoria distinta a la actual.
     * 
     * @return Una zona diferente de la que ocupa actualmente
     */
    private ZonaUpsideDown elegirSiguienteZona() {
        ZonaUpsideDown siguiente;

        // Verifiquemos que la siguiente zona que se tiene que elegir no
        // sea la misma que en la que ya se encuentra el Demogorgon.
        do {
            siguiente = zonas[(int) (Math.random() * zonas.length)];
        } while (siguiente == zonaActual);

        return siguiente;
    }

    /**
     * Obtiene el nombre del Demogorgon.
     * 
     * @return Nombre del Demogorgon
     */
    public String getNombre() {
        return this.nombre;
    }

    /**
     * Obtiene el número total de capturas realizadas.
     * 
     * @return Número de capturas
     */
    public int getCapturas() {
        return this.contCapturas;
    }

    /**
     * Obtiene la zona actual del Demogorgon.
     * 
     * @return Zona actual, o null si todavía no ha entrado en ninguna
     */
    public ZonaUpsideDown getZonaActual() {
        return this.zonaActual;
    }
}
