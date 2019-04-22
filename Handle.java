package trabalhin;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import javax.imageio.ImageIO;

public class Handle implements Runnable{
	Socket clientSocket;
	BufferedReader proxyReader;
	BufferedWriter proxyWriter;
	private Thread clienteServidor;
	//se comunica com o cliente
	public Handle(Socket clientSocket){
		this.clientSocket = clientSocket;
		try{
			this.clientSocket.setSoTimeout(10*1000);
			proxyReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			proxyWriter = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
		} 
		catch (IOException exception) {
			exception.printStackTrace();
		}
	}
	
	@Override
	public void run(){
		// Get Request from client
				String pedido;
				try{
					pedido = proxyReader.readLine();
				} catch (IOException exception) {
					exception.printStackTrace();
					System.out.println("Erro lendo pedido do cliente\n");
					return;
				}
				System.out.println("Pedido recebido do cliente" + pedido);
				//primeiro temos que tratar a string de pedido, separando a url do resto
				String pedidoSep = pedido.substring(0,pedido.indexOf(' '));
				String url = pedido.substring(pedido.indexOf(' ')+1);
				//pegue somente a url
				url = url.substring(0, url.indexOf(' '));
				//se a url nao tiver a preposicao http:// coloque
				if(!url.substring(0,7).equals("http://")) {
					url = "http://" + url;
				}
				if(Proxy.isBlocked(url)){
					System.out.println("Pedido de site bloqueado : " + url);
					blockedSiteRequested();
					return;
				}
				if(pedidoSep.equals("CONNECT")){
					System.out.println("Pedido HTTPS para : " + url + "\n");
					handleHTTPSRequest(url);
				} 
				else {
					File arq;
					//verifique se o arquivo esta em cache
					if(Proxy.buscaCache(url) != null) {
						arq = Proxy.buscaCache(url);
						System.out.println("Copia de : " +url + "encontrada");
						enviaCache(arq);
					}
					else {
						System.out.println("Copia de : " +url + "nao encontrada");
						enviaSemCache(url);
					}
				}		
	}
	
	private void enviaCache(File arq) {
		try {
			//verifica se o arquivo eh uma imagem
			String resposta;
			String tipoArq = arq.getName().substring(arq.getName().lastIndexOf('.'));
			if(tipoArq.contains(".jpg") || tipoArq.contains(".gif") || 
				tipoArq.contains(".jpeg") ||tipoArq.contains(".png")) {
				BufferedImage imagem = ImageIO.read(arq);
				if(imagem == null) {
					resposta = "HTTP/1.0 404 NOT FOUND\n";
					proxyWriter.write(resposta);
					proxyWriter.flush();
				}
				else {
					resposta = "HTTP/1.0 200 OK\n";
					proxyWriter.write(resposta);
					proxyWriter.flush();
					ImageIO.write(imagem, tipoArq.substring(1), clientSocket.getOutputStream());
				}
			}
			//se nao for imagem, eh texto
			else {
				BufferedReader leitorArq = new BufferedReader(new InputStreamReader(new FileInputStream(arq)));
				resposta = "HTTP/1.0 200 OK\n";
				proxyWriter.write(resposta);
				proxyWriter.flush();
				String linha;
				while((linha = leitorArq.readLine()) != null) {
					proxyWriter.write(linha);
				}
				proxyWriter.flush();
				if(leitorArq != null) {
					leitorArq.close();
				}
			}
			if(proxyWriter != null) {
				proxyWriter.close();
			}
		}catch(IOException exception) {
			exception.printStackTrace();
		}
	}
	private void enviaSemCache(String url) {
		try{
			
			// Compute a logical file name as per schema
			// This allows the files on stored on disk to resemble that of the URL it was taken from
			int fileExtensionIndex = url.lastIndexOf(".");
			String fileExtension;

			// Get the type of file
			fileExtension = url.substring(fileExtensionIndex, url.length());

			// Get the initial file name
			String fileName = url.substring(0,fileExtensionIndex);


			// Trim off http://www. as no need for it in file name
			fileName = fileName.substring(fileName.indexOf('.')+1);

			// Remove any illegal characters from file name
			fileName = fileName.replace("/", "__");
			fileName = fileName.replace('.','_');
			
			// Trailing / result in index.html of that directory being fetched
			if(fileExtension.contains("/")){
				fileExtension = fileExtension.replace("/", "__");
				fileExtension = fileExtension.replace('.','_');
				fileExtension += ".html";
			}
		
			fileName = fileName + fileExtension;



			// Attempt to create File to cache to
			boolean caching = true;
			File fileToCache = null;
			BufferedWriter fileToCacheBW = null;

			try{
				// Create File to cache 
				fileToCache = new File("cached/" + fileName);

				if(!fileToCache.exists()){
					fileToCache.createNewFile();
				}

				// Create Buffered output stream to write to cached copy of file
				fileToCacheBW = new BufferedWriter(new FileWriter(fileToCache));
			}
			catch (IOException e){
				System.out.println("Não foi possível adicionar a cache: " + fileName);
				caching = false;
				e.printStackTrace();
			} catch (NullPointerException e) {
				System.out.println("Erro abrindo arquivo");
			}





			// Check if file is an image
			if((fileExtension.contains(".png")) || fileExtension.contains(".jpg") ||
					fileExtension.contains(".jpeg") || fileExtension.contains(".gif")){
				// Create the URL
				URL remoteURL = new URL(url);
				BufferedImage image = ImageIO.read(remoteURL);

				if(image != null) {
					// Cache the image to disk
					ImageIO.write(image, fileExtension.substring(1), fileToCache);

					// Send response code to client
					String line = "HTTP/1.0 200 OK\n" +
							"Proxy-agent: ProxyServer/1.0\n" +
							"\r\n";
					proxyWriter.write(line);
					proxyWriter.flush();

					// Send them the image data
					ImageIO.write(image, fileExtension.substring(1), clientSocket.getOutputStream());

				// No image received from remote server
				} else {
					System.out.println("Enviando 404 ao cliente pois a imagem não foi recebida do servidor"
							+ fileName);
					String error = "HTTP/1.0 404 NOT FOUND\n" +
							"Proxy-agent: ProxyServer/1.0\n" +
							"\r\n";
					proxyWriter.write(error);
					proxyWriter.flush();
					return;
				}
			} 

			// File is a text file
			else {
								
				// Create the URL
				URL remoteURL = new URL(url);
				// Create a connection to remote server
				HttpURLConnection proxyToServerCon = (HttpURLConnection)remoteURL.openConnection();
				proxyToServerCon.setRequestProperty("Content-Type", 
						"application/x-www-form-urlencoded");
				proxyToServerCon.setRequestProperty("Content-Language", "en-US");  
				proxyToServerCon.setUseCaches(false);
				proxyToServerCon.setDoOutput(true);
			
				// Create Buffered Reader from remote Server
				BufferedReader proxyToServerBR = new BufferedReader(new InputStreamReader(proxyToServerCon.getInputStream()));
				

				// Send success code to client
				String line = "HTTP/1.0 200 OK\n" +
						"Proxy-agent: ProxyServer/1.0\n" +
						"\r\n";
				proxyWriter.write(line);
				
				
				// Read from input stream between proxy and remote server
				while((line = proxyToServerBR.readLine()) != null){
					// Send on data to client
					proxyWriter.write(line);

					// Write to our cached copy of the file
					if(caching){
						fileToCacheBW.write(line);
					}
				}
				
				// Ensure all data is sent by this point
				proxyWriter.flush();

				// Close Down Resources
				if(proxyToServerBR != null){
					proxyToServerBR.close();
				}
			}


			if(caching){
				// Ensure data written and add to our cached hash maps
				fileToCacheBW.flush();
				Proxy.addCache(url, fileToCache);
			}

			// Close down resources
			if(fileToCacheBW != null){
				fileToCacheBW.close();
			}

			if(proxyWriter != null){
				proxyWriter.close();
			}
		} 

		catch (Exception e){
			e.printStackTrace();
		}		
	}
	private void handleHTTPSRequest(String urlString){
		// Extract the URL and port of remote 
		String url = urlString.substring(7);
		String pieces[] = url.split(":");
		url = pieces[0];
		int port  = Integer.valueOf(pieces[1]);

		try{
			// Only first line of HTTPS request has been read at this point (CONNECT *)
			// Read (and throw away) the rest of the initial data on the stream
			for(int i=0;i<5;i++){
				proxyReader.readLine();
			}

			// Get actual IP associated with this URL through DNS
			InetAddress address = InetAddress.getByName(url);
			
			// Open a socket to the remote server 
			Socket proxyToServerSocket = new Socket(address, port);
			proxyToServerSocket.setSoTimeout(5000);

			// Send Connection established to the client
			String line = "HTTP/1.0 200 Connection established\r\n" +
					"Proxy-Agent: ProxyServer/1.0\r\n" +
					"\r\n";
			proxyWriter.write(line);
			proxyWriter.flush();
			
			
			
			// Client and Remote will both start sending data to proxy at this point
			// Proxy needs to asynchronously read data from each party and send it to the other party


			//Create a Buffered Writer betwen proxy and remote
			BufferedWriter proxyToServerBW = new BufferedWriter(new OutputStreamWriter(proxyToServerSocket.getOutputStream()));

			// Create Buffered Reader from proxy and remote
			BufferedReader proxyToServerBR = new BufferedReader(new InputStreamReader(proxyToServerSocket.getInputStream()));



			// Create a new thread to listen to client and transmit to server
			ClientToServerHttpsTransmit clientToServerHttps = 
					new ClientToServerHttpsTransmit(clientSocket.getInputStream(), proxyToServerSocket.getOutputStream());
			
			clienteServidor = new Thread(clientToServerHttps);
			clienteServidor.start();
			
			
			// Listen to remote server and relay to client
			try {
				byte[] buffer = new byte[4096];
				int read;
				do {
					read = proxyToServerSocket.getInputStream().read(buffer);
					if (read > 0) {
						clientSocket.getOutputStream().write(buffer, 0, read);
						if (proxyToServerSocket.getInputStream().available() < 1) {
							clientSocket.getOutputStream().flush();
						}
					}
				} while (read >= 0);
			}
			catch (SocketTimeoutException e) {
				
			}
			catch (IOException e) {
				e.printStackTrace();
			}


			// Close Down Resources
			if(proxyToServerSocket != null){
				proxyToServerSocket.close();
			}

			if(proxyToServerBR != null){
				proxyToServerBR.close();
			}

			if(proxyToServerBW != null){
				proxyToServerBW.close();
			}

			if(proxyWriter != null){
				proxyWriter.close();
			}
			
			
		} catch (SocketTimeoutException e) {
			String line = "HTTP/1.0 504 Timeout Occured after 10s\n" +
					"User-Agent: ProxyServer/1.0\n" +
					"\r\n";
			try{
				proxyWriter.write(line);
				proxyWriter.flush();
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		} 
		catch (Exception e){
			System.out.println("Erro de HTTPS : " + urlString );
			e.printStackTrace();
		}
	}
	class ClientToServerHttpsTransmit implements Runnable{
		
		InputStream proxyToClientIS;
		OutputStream proxyToServerOS;
		
		/**
		 * Creates Object to Listen to Client and Transmit that data to the server
		 * @param proxyToClientIS Stream that proxy uses to receive data from client
		 * @param proxyToServerOS Stream that proxy uses to transmit data to remote server
		 */
		public ClientToServerHttpsTransmit(InputStream proxyToClientIS, OutputStream proxyToServerOS) {
			this.proxyToClientIS = proxyToClientIS;
			this.proxyToServerOS = proxyToServerOS;
		}

		@Override
		public void run(){
			try {
				// Read byte by byte from client and send directly to server
				byte[] buffer = new byte[4096];
				int read;
				do {
					read = proxyToClientIS.read(buffer);
					if (read > 0) {
						proxyToServerOS.write(buffer, 0, read);
						if (proxyToClientIS.available() < 1) {
							proxyToServerOS.flush();
						}
					}
				} while (read >= 0);
			}
			catch (SocketTimeoutException ste) {
				String line = "HTTP/1.0 504 Timeout Occured after 10s\n" +
						"User-Agent: ProxyServer/1.0\n" +
						"\r\n";
				try{
					proxyWriter.write(line);
					proxyWriter.flush();
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
			}
			catch (IOException e) {
				System.out.println("TIMEOUT de leitura da conexão Proxy para o cliente HTTPS");
				e.printStackTrace();
			}
		}
	}
/**
 * This method is called when user requests a page that is blocked by the proxy.
 * Sends an access forbidden message back to the client
 */
	private void blockedSiteRequested(){
		try {
			BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
			String line = "HTTP/1.0 403 Access Forbidden \n" +
				"User-Agent: ProxyServer/1.0\n" +
				"\r\n";
			bufferedWriter.write(line);
			bufferedWriter.flush();
		} catch (IOException e) {
			System.out.println("Erro escrevendo no buffer do cliente quando site bloqueado foi pedido");
			e.printStackTrace();
		}
	}
}

