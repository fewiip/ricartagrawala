import java.util.*;
import java.net.*;
import java.io.*;



public class AgrawaRicart{
	public static List<Process> cs_list;

	public static class Process {
		String id;
		int timestamp;
		Process(String id, int timestamp){
			this.id = id;
			this.timestamp = timestamp;
		}
	}

	/*
	Estados dos processos: 
	1 - Released - processo fora da Sessao Critica
	2 - Wanted - deseja entrar na Sessao Critica
	3 - Held - encontra-se na Sessao Critica
	
	*/
	static int estado;
	static String id;
	static int porta;
	static int timestamp;
	static int votes = 0;
	
	public static void main(String args[]){ 
		//um terminal: java Ric.java A0teste0 228.5.6.7
		//outro terminal: java Ric.java A0escute0 228.5.6.7
		// args give message contents and destination multicast group (e.g. "228.5.6.7")
		//224.0.0.0 a 239.255.255.255

		//int[] ports = {8111,8222,8333,8444};
		estado = 0;		
		id = (args[0].split("0"))[0];
		if (id.equals("A")) {
			porta = 8111;
		} else if (id.equals("B")) {
			porta = 8222;
		} else if (id.equals("C")) {
			porta = 8333;
		}
		System.out.println("id: " + id);
		
		cs_list = new ArrayList<>();
		
		MulticastThread multicastThread = new MulticastThread(args[0], args[1]);
		TCPServerThread tcpserverThread = new TCPServerThread();
		//TCPClientThread tcpclientThread = new TCPClientThread("destino","mensagem");

	}		      	
	
	public static void cs(){
		//pubilca no multicast o pedido 
		
		//espera receber todos os OKs
		
		while (votes < 2) {
				
		}
		
		//entra na SC 

		//sai da SC 
		
		//se a lista de processos for nao vazia, ele manda mensagem TCP pro proximo processo 
		
		
		votes = 0;
	}
	
}

/*

Tipo1 objeto = new Tipo2();
public class MinhaThreadRunnable implemens Runnable {
	private String nome;
	private int valorAdormecida;

	public MinhaThreadRunnable (String nome, int valorAdormecida) {
		this.nome = nome;
		this.valorAdomercida = valorAdomercida;
		//nao precisa mais do start 
		//start();
	}

	public void run(){
		System.out.println(nome + "foi iniciada");
		try {
			//faz as coisas aqui 
		} catch (InterruptedException e) {
			System.out.println();	
		}
	}
	
}

public static void main (String []args){
	MinhaThread thread1 = new MinhaThread("Thread1",500);
	MinhaThread thread2 = new MinhaThread("Thread2", 900);
}
*/

final class MulticastThread implements Runnable {
	MulticastSocket s = null;
	String comandoterminal;
	String mensagem;
	String[] comandos;
	byte[] buffer = new byte[1000];
	String id;
	int porta;
	String groupName;
	InetAddress group;

	public MulticastThread (String comandoterminal,String groupName) {
		this.comandoterminal = comandoterminal;
		this.groupName = groupName;
		
		//outra maneira de iniciar a thread
		Thread t = new Thread(this);
		t.start();
	}

	public void run () {

		try {
			
			InetAddress group = InetAddress.getByName(groupName);
			s = new MulticastSocket(6789);
			s.joinGroup(group);
			byte [] m = comandoterminal.getBytes();
			
			//mensagem que eu vou enviar 
			DatagramPacket messageOut = new DatagramPacket(m, m.length, group, 6789);

			//aqui que ele envia a mensagem pro grupo 
			s.send(messageOut);	

			//tem que separar os comandos com um caractere que nao de problema
			//no caso escolhido foi o 0
			comandos =  comandoterminal.split("0");
			id = comandos[0];
			
			//fica escutando o canal multicast 
			while(true){
				DatagramPacket messageIn = new DatagramPacket(buffer, buffer.length);
				s.receive(messageIn);
				mensagem = new String(messageIn.getData());
				comandos = mensagem.split("0");
				//System.out.println("comando: " + comandos[1]);

				if (comandos[1].equals("teste")){
					
					if (!comandos[0].equals(id) ) {
						//manda unicast pro alvo
						//pra nao ser tipo B ouviu e vai responder pra B 
						System.out.println(AgrawaRicart.id + " ouviu em multicast e vai responder em unicast(TCP) pra " + comandos[0]);
						//TCPClientThread tcpclientThread = new TCPClientThread("destino","mensagem");
						TCPClientThread tcpclientThread = new TCPClientThread(comandos[0],AgrawaRicart.id + "0OK0");
					} 
					
				}
				System.out.println("MultiCast Received:" + new String(messageIn.getData()));

			}
		//s.leaveGroup(group);		
		}catch(SocketException e ){
			System.out.println("Socket: " + e.getMessage());
		}catch (IOException e){
			System.out.println("IO: " + e.getMessage());
		}finally {
			if(s != null) s.close();
		}
	}
}

public class TCPServerThread implements Runnable {
	
	public TCPServerThread () {
		//outra maneira de iniciar a thread
		Thread t = new Thread(this);
		t.start();
	}

	public void run () {
		try{
			int serverPort = AgrawaRicart.porta;//7896; // the server port
			ServerSocket listenSocket = new ServerSocket(serverPort);
			while(true) {
				Socket clientSocket = listenSocket.accept();
				Connection c = new Connection(clientSocket);
			}
		} catch(IOException e) {System.out.println("Listen socket:"+e.getMessage());}
	}
}

class Connection extends Thread {
	DataInputStream in;
	DataOutputStream out;
	Socket clientSocket;
	public Connection (Socket aClientSocket) {
		try {
			clientSocket = aClientSocket;
			in = new DataInputStream( clientSocket.getInputStream());
			out =new DataOutputStream( clientSocket.getOutputStream());
			this.start();
		} catch(IOException e) {System.out.println("Connection:"+e.getMessage());}
	}
	public void run(){
		try {			                 // an echo server

			String data = in.readUTF();	                  // read a line of data from the stream
			System.out.println("Mensagem recebida de um cliente TCP: " + data);
			out.writeUTF(data);
		}catch (EOFException e){System.out.println("EOF:"+e.getMessage());
		} catch(IOException e) {System.out.println("readline:"+e.getMessage());
		} finally{ try {clientSocket.close();}catch (IOException e){/*close failed*/}}
		

	}
} 

//quando eu quiser mandar uma mensagem eu uso esse!
final class TCPClientThread implements Runnable {
	//arguments supply message and hostname
	String id;
	int portadestino;
	String mensagem;

	public TCPClientThread (String destino, String mensagem) {
		
		if (destino.equals("A")) {
			portadestino = 8111;
		} else if (destino.equals("B")) {
			portadestino = 8222;
		} else if (destino.equals("C")) {
			portadestino = 8333;
		}

		this.mensagem = mensagem;
		//outra maneira de iniciar a thread
		Thread t = new Thread(this);
		t.start();
	}

	public void run () {
		
		Socket s = null;
		try{
			int serverPort = portadestino;
			s = new Socket("127.0.0.1", serverPort);    //new Socket(args[1], serverPort);    
			DataInputStream in = new DataInputStream( s.getInputStream());
			DataOutputStream out =new DataOutputStream( s.getOutputStream());
			out.writeUTF(mensagem);//(args[0]);      	// UTF is a string encoding see Sn. 4.4
			String data = in.readUTF();	    // read a line of data from the stream
			//System.out.println("Received: "+ data) ; 
			System.out.println("servidor recebeu!");
		}catch (UnknownHostException e){System.out.println("Socket:"+e.getMessage());
		}catch (EOFException e){System.out.println("EOF:"+e.getMessage());
		}catch (IOException e){System.out.println("readline:"+e.getMessage());
		}finally {if(s!=null) try {s.close();}catch (IOException e){System.out.println("close:"+e.getMessage());}}

	}
}
/*


final class TCPClientThread implements Runnable {

	void run () {
		Socket s = null;
		try{
			int serverPort = 7896;
			s = new Socket(args[1], serverPort);    
			DataInputStream in = new DataInputStream( s.getInputStream());
			DataOutputStream out =new DataOutputStream( s.getOutputStream());
			out.writeUTF(args[0]);      	// UTF is a string encoding see Sn. 4.4
			String data = in.readUTF();	    // read a line of data from the stream
			System.out.println("Received: "+ data) ; 
		}catch (UnknownHostException e){System.out.println("Socket:"+e.getMessage());
		}catch (EOFException e){System.out.println("EOF:"+e.getMessage());
		}catch (IOException e){System.out.println("readline:"+e.getMessage());
		}finally {if(s!=null) try {s.close();}catch (IOException e){System.out.println("close:"+e.getMessage());}}

	}
	
	while (true){
		//fica escutando o canal pro tcp 
		// Listen for a TCP connection request.
		Socket connection = socket.accept();
		
		// Construct an object to process the HTTP request message.
		TCPServer sendTCP = new TCPServer(connection);
		
		// Create a new thread to process the request.
		Thread thread = new Thread(request);
		
		// Start the thread.
		thread.start();
	}

}

final class TCPServer implements Runnable {


}
*/
