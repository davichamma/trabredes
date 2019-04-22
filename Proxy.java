package trabalhin;
import java.io.*;
import java.net.*;
import java.util.*;

public class Proxy implements Runnable{
	private ServerSocket serverSocket;
	private volatile boolean running = true;
	static HashMap<String, File> cache;
	static HashMap<String, String> blockedSites;
	static ArrayList<Thread> openThreads;
	static int tamanhoCache;
	static LRUCache<String, File> ccache;
	public static void main(String[] args) {
		// Cria uma instancia de proxy e começa a escutar na porta desejada
		Proxy myProxy = new Proxy(Integer.parseInt(args[0]));
		//salvar o valor do tamanho da cache em variavel de instancia para ser utilizado em outro metodo
		tamanhoCache = Integer.parseInt(args[1]);
		myProxy.listen();	
	}
	public Proxy(int porta) {
		ccache = new LRUCache<>(tamanhoCache);
		//a fim de identificar as threads criaremos uma array
		openThreads = new ArrayList<>();
		new Thread(this).start();
		try{
			// Load in cached sites from file
			File cachedSites = new File("cachedSites.txt");
			if(!cachedSites.exists()){
				System.out.println("No cached sites found - creating new file");
				cachedSites.createNewFile();
			} else {
				FileInputStream fileInputStream = new FileInputStream(cachedSites);
				ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
				ccache = (LRUCache<String,File>)objectInputStream.readObject();
				fileInputStream.close();
				objectInputStream.close();
			}

			// Load in blocked sites from file
			File blockedSitesTxtFile = new File("blockedSites.txt");
			if(!blockedSitesTxtFile.exists()){
				System.out.println("No blocked sites found - creating new file");
				blockedSitesTxtFile.createNewFile();
			} else {
				FileInputStream fileInputStream = new FileInputStream(blockedSitesTxtFile);
				ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
				blockedSites = (HashMap<String, String>)objectInputStream.readObject();
				fileInputStream.close();
				objectInputStream.close();
			}
		} catch (IOException e) {
			System.out.println("Error loading previously cached sites file");
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			System.out.println("Class not found loading in preivously cached sites file");
			e.printStackTrace();
		}
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
				//adiciona uma referencia para cada thread para encerrar o servidor 
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
		return ccache.get(url); //url é uma string a qual possui um arquivo na hash
	}
	//Adiciona arquivo na cache
	public static void addCache(String url, File arquivo){
		ccache.put(url, arquivo);
	}
	public static boolean isBlocked (String url){
		if(blockedSites.get(url) != null){
			return true;
		} else {
			return false;
		}
	}
	private void closeServer(){
		System.out.println("\nFechando servidor...");
		running = false;
		try{
			FileOutputStream fileOutputStream = new FileOutputStream("cachedSites.txt");
			ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);

			objectOutputStream.writeObject(ccache);
			objectOutputStream.close();
			fileOutputStream.close();
			System.out.println("Sites em cache salvos.");

			FileOutputStream fileOutputStream2 = new FileOutputStream("blockedSites.txt");
			ObjectOutputStream objectOutputStream2 = new ObjectOutputStream(fileOutputStream2);
			objectOutputStream2.writeObject(blockedSites);
			objectOutputStream2.close();
			fileOutputStream2.close();
			System.out.println("Lista dos sites bloqueados salva.");
			try{
				// fecha todas as threads 
				for(Thread thread : openThreads){
					if(thread.isAlive()){
						System.out.print("Esperando "+  thread.getId()+" ser fechado...");
						thread.join();
						System.out.println(" encerrado");
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			} catch (IOException e) {
				System.out.println("Erro salvando sites em cache/bloqueados");
				e.printStackTrace();
			}

			// fecha o socket do servidor
			try{
				System.out.println("Encerrando conexão");
				serverSocket.close();
			} catch (Exception e) {
				System.out.println("Erro fechando socket de servidor do proxy");
				e.printStackTrace();
			}

		}
	@Override
	public void run() {
		Scanner sc = new Scanner(System.in);
		String comando;
		while(running) {
			System.out.println("Digite um site para ser bloqueado ou utilize os comandos : 'bloqueados' para ver os sites bloqueados, 'cached' para ver os sites em cache, 'close' para fechar o servidor.");
			comando = sc.nextLine();
			if(comando.equals("close")) {
				running = false;
				closeServer();
			}
			else if(comando.toLowerCase().equals("bloqueados")){
				System.out.println("\nSites bloqueados atualmente");
				for(String key : blockedSites.keySet()){
					System.out.println(key);
				}
				System.out.println();
			} 

			else if(comando.toLowerCase().equals("cached")){
				System.out.println("\nSites na cache atualmente");
				for(String key : cache.keySet()){
					System.out.println(key);
				}
				System.out.println();
			}
			else {
				blockedSites.put(comando, comando);
				System.out.println("\n" + comando + " blocked successfully \n");
			}
		}
		sc.close();
	}
	
}
