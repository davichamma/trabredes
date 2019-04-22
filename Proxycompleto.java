package copiadores;

import java.io.*;
import java.net.*;
import java.util.*;


/**
 * The Proxy creates a Server Socket which will wait for connections on the specified port.
 * Once a connection arrives and a socket is accepted, the Proxy creates a RequestHandler object
 * on a new thread and passes the socket to it to be handled.
 * This allows the Proxy to continue accept further connections while others are being handled.
 * 
 * The Proxy class is also responsible for providing the dynamic management of the proxy through the console 
 * and is run on a separate thread in order to not interrupt the acceptance of socket connections.
 * This allows the administrator to dynamically block web sites in real time. 
 * 
 * The Proxy server is also responsible for maintaining cached copies of the any websites that are requested by
 * clients and this includes the HTML markup, images, css and js files associated with each webpage.
 * 
 * Upon closing the proxy server, the HashMaps which hold cached items and blocked sites are serialized and
 * written to a file and are loaded back in when the proxy is started once more, meaning that cached and blocked
 * sites are maintained.
 *
 */
public class Proxy implements Runnable{


	// Main method for the program
	public static void main(String[] args) {
		// Create an instance of Proxy and begin listening for connections
		int a = Integer.parseInt(args[0]);
		Proxy myProxy = new Proxy(a);
		myProxy.listen();	
	}


	private ServerSocket serverSocket;

	/**
	 * Semaphore for Proxy and Consolee Management System.
	 */
	private volatile boolean running = true;


	/**
	 * Data structure for constant order lookup of cache items.
	 * Key: URL of page/image requested.
	 * Value: File in storage associated with this key.
	 */
	static HashMap<String, File> cache;

	/**
	 * Data structure for constant order lookup of blocked sites.
	 * Key: URL of page/image requested.
	 * Value: URL of page/image requested.
	 */
	static HashMap<String, String> blockedSites;

	/**
	 * ArrayList of threads that are currently running and servicing requests.
	 * This list is required in order to join all threads on closing of server
	 */
	static ArrayList<Thread> servicingThreads;



	/**
	 * Create the Proxy Server
	 * @param port Port number to run proxy server from.
	 */
	public Proxy(int port) {

		// Load in hash map containing previously cached sites and blocked Sites
		cache = new HashMap<>();
		blockedSites = new HashMap<>();

		// Create array list to hold servicing threads
		servicingThreads = new ArrayList<>();

		// Start dynamic manager on a separate thread.
		new Thread(this).start();	// Starts overriden run() method at bottom

		try{
			// Load in cached sites from file
			File cachedSites = new File("cachedSites.txt");
			if(!cachedSites.exists()){
				System.out.println("Não foram encontrados sites em cache - criando novo arquivo");
				cachedSites.createNewFile();
			} else {
				FileInputStream fileInputStream = new FileInputStream(cachedSites);
				ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
				cache = (HashMap<String,File>)objectInputStream.readObject();
				fileInputStream.close();
				objectInputStream.close();
			}

			// Load in blocked sites from file
			File blockedSitesTxtFile = new File("blockedSites.txt");
			if(!blockedSitesTxtFile.exists()){
				System.out.println("Não foram encontrados sites bloqueados - criando novo arquivo");
				blockedSitesTxtFile.createNewFile();
			} else {
				FileInputStream fileInputStream = new FileInputStream(blockedSitesTxtFile);
				ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
				blockedSites = (HashMap<String, String>)objectInputStream.readObject();
				fileInputStream.close();
				objectInputStream.close();
			}
		} catch (IOException e) {
			System.out.println("Erro carregando arquivo contendo sites em cache");
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			System.out.println("Classe não encontrada ao carregar arquivo contendo sites em cache");
			e.printStackTrace();
		}

		try {
			// Create the Server Socket for the Proxy 
			serverSocket = new ServerSocket(port);

			// Set the timeout
			//serverSocket.setSoTimeout(100000);	// debug
			System.out.println("Esperando o cliente na porta " + serverSocket.getLocalPort() + "...");
			running = true;
		} 

		// Catch exceptions associated with opening socket
		catch (SocketException se) {
			System.out.println("Socket Exception when connecting to client");
			se.printStackTrace();
		}
		catch (SocketTimeoutException ste) {
			System.out.println("Timeout occured while connecting to client");
		} 
		catch (IOException io) {
			System.out.println("IO exception when connecting to client");
		}
	}


	/**
	 * Listens to port and accepts new socket connections. 
	 * Creates a new thread to handle the request and passes it the socket connection and continues listening.
	 */
	public void listen(){

		while(running){
			try {
				// serverSocket.accpet() Blocks until a connection is made
				Socket socket = serverSocket.accept();
				
				// Create new Thread and pass it Runnable RequestHandler
				Thread thread = new Thread(new RequestHandler(socket));
				
				// Key a reference to each thread so they can be joined later if necessary
				servicingThreads.add(thread);
				
				thread.start();	
			} catch (SocketException e) {
				// Socket exception is triggered by management system to shut down the proxy 
				System.out.println("Servidor fechado");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}


	/**
	 * Saves the blocked and cached sites to a file so they can be re loaded at a later time.
	 * Also joins all of the RequestHandler threads currently servicing requests.
	 */
	private void closeServer(){
		System.out.println("\nFechando servidor...");
		running = false;
		try{
			FileOutputStream fileOutputStream = new FileOutputStream("cachedSites.txt");
			ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);

			objectOutputStream.writeObject(cache);
			objectOutputStream.close();
			fileOutputStream.close();
			System.out.println("Sites em cache salvos");

			FileOutputStream fileOutputStream2 = new FileOutputStream("blockedSites.txt");
			ObjectOutputStream objectOutputStream2 = new ObjectOutputStream(fileOutputStream2);
			objectOutputStream2.writeObject(blockedSites);
			objectOutputStream2.close();
			fileOutputStream2.close();
			System.out.println("Lista de sites bloqueados salva");
			try{
				// Close all servicing threads
				for(Thread thread : servicingThreads){
					if(thread.isAlive()){
						System.out.print("Esperando "+  thread.getId()+" ser encerrada...");
						thread.join();
						System.out.println(" Thread encerrada");
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			} catch (IOException e) {
				System.out.println("Erro salvando sites em cache/bloqueados");
				e.printStackTrace();
			}

			// Close Server Socket
			try{
				System.out.println("Encerrando conexão");
				serverSocket.close();
			} catch (Exception e) {
				System.out.println("Erro fechando o socket do servidor");
				e.printStackTrace();
			}

		}


		/**
		 * Looks for File in cache
		 * @param url of requested file 
		 * @return File if file is cached, null otherwise
		 */
		public static File getCachedPage(String url){
			return cache.get(url);
		}


		/**
		 * Adds a new page to the cache
		 * @param urlString URL of webpage to cache 
		 * @param fileToCache File Object pointing to File put in cache
		 */
		public static void addCachedPage(String urlString, File fileToCache){
			cache.put(urlString, fileToCache);
		}

		/**
		 * Check if a URL is blocked by the proxy
		 * @param url URL to check
		 * @return true if URL is blocked, false otherwise
		 */
		public static boolean isBlocked (String url){
			if(blockedSites.get(url) != null){
				return true;
			} else {
				return false;
			}
		}




		/**
		 * Creates a management interface which can dynamically update the proxy configurations
		 * 		blocked : Lists currently blocked sites
		 *  	cached	: Lists currently cached sites
		 *  	close	: Closes the proxy server
		 *  	*		: Adds * to the list of blocked sites
		 */
		@Override
		public void run() {
			Scanner scanner = new Scanner(System.in);

			String command;
			while(running){
				System.out.println("Digite novo site a ser bloqueado, ou digite \"blocked\" para ver os sites bloqueados, \"cached\" para ver os sites em cache, ou \"close\" para fechar o servidor.");
				command = scanner.nextLine();
				if(command.toLowerCase().equals("blocked")){
					System.out.println("\nLista de sites bloqueados atualmente");
					for(String key : blockedSites.keySet()){
						System.out.println(key);
					}
					System.out.println();
				} 

				else if(command.toLowerCase().equals("cached")){
					System.out.println("\nLista de sites em cache atualmente");
					for(String key : cache.keySet()){
						System.out.println(key);
					}
					System.out.println();
				}


				else if(command.equals("close")){
					running = false;
					closeServer();
				}


				else {
					blockedSites.put(command, command);
					System.out.println("\n" + command + " bloqueado com sucesso \n");
				}
			}
			scanner.close();
		} 

	}

