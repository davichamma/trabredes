package trabalhin;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class Proxy implements Runnable{
	public static void main(String[] args) {
		// Cia uma instancia de proxy e começa a escutar na porta desejada
		Proxy myProxy = new Proxy(Integer.parseInt(args[0]));
		myProxy.listen();	
	}
	private ServerSocket serverSocket;
	private volatile boolean running = true;
	static HashMap<String, File> cache;
	static HashMap<String, String> blockedSites;
	static ArrayList<Thread> openThreads;
	
	public Proxy(int porta) {
		cache = new HashMap<>();
		//a fim de identificar as threads criaremos uma array
		openThreads = new ArrayList<>();
		new Thread(this).start();
		try {
			serverSocket = new ServerSocket(porta);
			System.out.println("Esperando por cliente na porta "+ porta + "...");
			running = true;
		}catch(SocketException e) {
			System.out.println("Socket Exception ao conectar com o cliente");
		}catch(SocketTimeoutException to) {
			System.out.println("Excedido o tempo limite de conexão");
		}catch(IOException io) {
			System.out.println("IO exception ao conectar com o cliente");
		}
		
	}
	//O servicor escuta na porta lida
	public void listen() {
		while(running) {
			try {
				//cria o socket do servidor
				Socket sockServ = serverSocket.accept();
				//cria uma thread para tratar os requisitos desse socket
				Thread trata = new Thread(new Handle(sockServ));
				openThreads.add(trata);
				trata.start();
			}catch(SocketException sockException) {
				System.out.println("Servidor fechado");
			}catch(IOException exception) {
				exception.printStackTrace();
			}
			
		}
	}
	//Busca um arquivo na cache
	public static File buscaCache(String url){
		return cache.get(url); //url é uma string a qual possui um arquivo na hash
	}
	//Adiciona arquivo na cache
	public static void addCache(String urlString, File arquivo){
		cache.put(urlString, arquivo);
	}
	@Override
	public void run() {
		Scanner sc = new Scanner(System.in);
		String comando;
		comando = sc.nextLine();
		while(running) {
			if(comando.equals("close")) {
				running = false;
			}
		}
		sc.close();
	}
}
