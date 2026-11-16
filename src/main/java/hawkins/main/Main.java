package hawkins.main;

import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

import hawkins.eventos.GestorEventos;
import hawkins.hilos.ControlSimulacion;
import hawkins.hilos.Demogorgon;
import hawkins.hilos.GeneradorEventos;
import hawkins.hilos.GeneradorNinos;
import hawkins.log.Log;
import hawkins.rmi.ServidorHawkins;
import hawkins.ui.VentanaCliente;
import hawkins.ui.VentanaServidor;
import hawkins.zonas.comunes.Portal;
import hawkins.zonas.mundoreal.CallePrincipal;
import hawkins.zonas.mundoreal.RadioWSQK;
import hawkins.zonas.mundoreal.SotanoByers;
import hawkins.zonas.mundoreal.ZonaHawkins;
import hawkins.zonas.upsidedown.Alcantarillado;
import hawkins.zonas.upsidedown.Bosque;
import hawkins.zonas.upsidedown.CentroComercial;
import hawkins.zonas.upsidedown.Colmena;
import hawkins.zonas.upsidedown.Laboratorio;
import hawkins.zonas.upsidedown.ZonaUpsideDown;

public class Main {
    public static void main(String[] args) {
        // Creamos el Logger para registrar los eventos del sistema
        Log logger = new Log("hawkins.txt");

        // Creamos las zonas del mundo real (Hawkins)
        CallePrincipal callePrincipal = new CallePrincipal("CALLE_PRINCIPAL");
        SotanoByers sotanoByers = new SotanoByers("SOTANO_BYERS");
        RadioWSQK radioWSQK = new RadioWSQK("RADIO_WSQK");

        ZonaHawkins[] zonasHawkins = {
                callePrincipal,
                sotanoByers,
                radioWSQK
        };

        // Creamos las zonas del UpsideDown
        Bosque bosque = new Bosque("BOSQUE");
        Laboratorio laboratorio = new Laboratorio("LABORATORIO");
        CentroComercial centroComercial = new CentroComercial("CENTRO_COMERCIAL");
        Alcantarillado alcantarillado = new Alcantarillado("ALCANTARILLADO");

        ZonaUpsideDown[] zonas = {
                bosque,
                laboratorio,
                centroComercial,
                alcantarillado
        };

        // Creamos una lista de Demogorgones activos (inicialmente vacía)
        List<Demogorgon> demogorgonsActivos = Collections.synchronizedList(new ArrayList<>());

        // Creamos el Controlador de Pausa para las simulaciones
        ControlSimulacion controlSimulacion = new ControlSimulacion();

        // Creamos la Colmena del UpsideDown
        Colmena colmena = new Colmena(
                zonas,
                demogorgonsActivos,
                logger,
                controlSimulacion);

        // Creamos el Gestor de Eventos para controlar eventos globales como el apagón
        GestorEventos gestorEventos = new GestorEventos(zonas, colmena, logger);

        // Le pasamos a la colmena el gestor de eventos por si tenemos que crear algún
        // nuevo Demogorgon.
        colmena.setGestorEventos(gestorEventos);

        // Creamos los portales para entrar al UpsideDown
        Portal[] portales = {
                new Portal("BOSQUE", 2, zonas[0], logger),
                new Portal("LABORATORIO", 3, zonas[1], logger),
                new Portal("CENTRO_COMERCIAL", 4, zonas[2], logger),
                new Portal("ALCANTARILLADO", 2, zonas[3], logger)
        };

        // Creamos al Demogorgon Alpa y añadimos a la lista de Demogorgones activos
        Demogorgon alpha = new Demogorgon(
                0,
                zonas,
                colmena,
                gestorEventos,
                logger,
                controlSimulacion);

        demogorgonsActivos.add(alpha);

        // Creamos el Generador de Niños
        GeneradorNinos generadorNinos = new GeneradorNinos(
                zonasHawkins,
                portales,
                gestorEventos,
                logger,
                controlSimulacion);

        // Creamos el hilo del generador de eventos
        GeneradorEventos generadorEventos = new GeneradorEventos(gestorEventos, controlSimulacion);

        // Creamos el servidor RMI para exponer la información de la simulación a
        // clientes externos
        try {
            // Le pasamos TODAS las referencias al servidor RMI para que pueda consultarlas
            ServidorHawkins servidorRMI = new ServidorHawkins(
                    callePrincipal,
                    sotanoByers,
                    radioWSQK,
                    bosque,
                    laboratorio,
                    centroComercial,
                    alcantarillado,
                    colmena,
                    portales,
                    gestorEventos,
                    demogorgonsActivos,
                    controlSimulacion);

            // Creamos el registro en el puerto 1099 y publicamos el objeto
            // "ServidorHawkins"
            LocateRegistry.createRegistry(1099);
            Naming.rebind("//localhost/ServidorHawkins", servidorRMI);
            System.out.println("Servidor RMI iniciado y escuchando en el puerto 1099.");

        } catch (Exception e) {
            System.err.println("Error al iniciar el servidor RMI: " + e.getMessage());
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            try {
                for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                    if ("Nimbus".equals(info.getName())) {
                        UIManager.setLookAndFeel(info.getClassName());
                        break;
                    }
                }
            } catch (Exception ex) {
                /* usar L&F por defecto */ 
            }

            VentanaServidor ventanaPrincipal = new VentanaServidor(
                    callePrincipal,
                    sotanoByers,
                    radioWSQK,
                    bosque,
                    laboratorio,
                    centroComercial,
                    alcantarillado,
                    colmena,
                    portales,
                    gestorEventos,
                    controlSimulacion);

            ventanaPrincipal.setVisible(true);

            // Lanzamos la ventana del cliente con un pequeño retraso para asegurar que el
            // servidor RMI esté
            // listo para aceptar conexiones antes de que el cliente intente conectarse.
            Timer timer = new Timer(1000, e -> {
                VentanaCliente ventanaCliente = new VentanaCliente();
                ventanaCliente.setLocationRelativeTo(null);
                ventanaCliente.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                ventanaCliente.setVisible(true);
            });
            timer.setRepeats(false); // que solo se dispare una vez
            timer.start();
        });

        // Iniciamos los hilos
        alpha.start();
        generadorNinos.start();
        generadorEventos.start();
    }
}