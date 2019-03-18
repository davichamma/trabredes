package trabalhin;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;
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
				if(pedido.equals("GET")) {
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
		
	}
}
