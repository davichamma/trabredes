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

public class Tratamento implements Runnable{
	Socket clientSocket;
	BufferedReader proxyToClientBr;
	BufferedWriter proxyToClientBw;
	private Thread httpsClientToServer;
	//se comunica com o cliente
	public Tratamento(Socket clientSocket){
		this.clientSocket = clientSocket;
		try{
			this.clientSocket.setSoTimeout(2000);
			proxyToClientBr = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			proxyToClientBw = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void run(){
		// Recebe pedido do cliente
				String requestString;
				try{
					requestString = proxyToClientBr.readLine();
				} catch (IOException e) {
					e.printStackTrace();
					System.out.println("Erro lendo pedido do cliente\n");
					return;
				}
				System.out.println("Pedido recebido do cliente" + requestString);
				//primeiro temos que tratar a string de pedido, separando a url do resto
				//caso haja o pedido de página
				if(requestString.equals("GET")) {
					//verifique se a página esta na cache
					
				}
	}
}
