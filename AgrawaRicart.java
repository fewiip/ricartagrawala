import java.util.*;
import java.net.*;
import java.io.*;
import java.time.*;
import java.time.format.*;


public class AgrawaRicart{
	public static List<Process> cs_list;

	/*
	Estados dos processos: 
	1 - Released - processo fora da Sessao Critica
	2 - Wanted - deseja entrar na Sessao Critica
	3 - Held - encontra-se na Sessao Critica
	*/

	/* Se o meu estado eh Wanted ou Held, eu vou adicionar a uma lista os processos que querem entrar */
	static int estado = 1;
	static String id;
	static int porta;
	static int timestamp;//2.147.483.647
	static int replies = 0;
	static Object lock = new Object();
	
	public static void main(String args[]){ 
		
		//um terminal: java Ric.java A0 228.5.6.7
		//outro terminal: java Ric.java A0escute0 228.5.6.7
		// args give message contents and destination multicast group (e.g. "228.5.6.7")
		//224.0.0.0 a 239.255.255.255

		//int[] ports = {8111,8222,8333,8444};
		
		id = (args[0].split("N"))[0];

		if (id.equals("A")) {
			porta = 8111;
		} else if (id.equals("B")) {
			porta = 8222;
		} else if (id.equals("C")) {
			porta = 8333;
		}
		
		cs_list = new ArrayList<>();
		
		MulticastThread multicastThread = new MulticastThread(args[0], args[1]);
		TCPServerThread tcpserverThread = new TCPServerThread();

		System.out.println("id: " + id);
		//requestCriticalSection();

		String opcao = null;
		Scanner scan = new Scanner(System.in);

		System.out.println("Atividade 1 - Sockets");
		System.out.println("Camila Puchta e Felipe Avelino");
		
		System.out.println("===================================");
		System.out.println("Opções:");

		while(true) {
			while(true){
				System.out.println("1: Entrar da seção critica");
				opcao = scan.nextLine();
				if (opcao.equals("1")) {
					break;
				}
				System.out.println("Opcao invalida");
			}
			requestCriticalSection();
			while(true){
				System.out.println("1: Sair da seção critica");
				opcao = scan.nextLine();
				if (opcao.equals("1")) {
					break;
				}
				System.out.println("Opcao invalida");
			}
			exitCriticalSection();
		}
		


	}		   
	
	
	
	//chamo essa funcao pelo menu
	public static void requestCriticalSection(){
		//publica no multicast o pedido 
		
		/*
		
		
		*/
		
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HHmmssSSS");

		//criando um timestamp
		AgrawaRicart.timestamp = Integer.parseInt(dtf.format(LocalTime.now()));
		//Random rand = new Random();
		//AgrawaRicart.timestamp = rand.nextInt(20000);
		try {
			InetAddress group = InetAddress.getByName("228.5.6.7");
			MulticastSocket s = new MulticastSocket(6789);
			s.joinGroup(group);
			//byte [] m = ("A0teste0").getBytes();//exemplo de mensagem
			byte [] m = (AgrawaRicart.id+"NrequestN"+AgrawaRicart.timestamp+"N").getBytes();
			
			//mensagem que eu vou enviar 
			DatagramPacket messageOut = new DatagramPacket(m, m.length, group, 6789);

			//aqui que ele envia a mensagem pro grupo 
			s.send(messageOut);	
		} catch(Exception e ) {
			System.out.println(e);
		}

		//muda o estado 
		AgrawaRicart.estado = 2;//Wanted!
		
		//espera receber todos os OKs
		/*
		while (true) {
			System.out.println(AgrawaRicart.replies);
				if (AgrawaRicart.replies == 2){
					break;
				}
		}*/
		try {
			synchronized (lock) {
				lock.wait();
			}
			System.out.println("hello");
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		System.out.println("Entrando na secao critica");

		//muda o estado 
		AgrawaRicart.estado = 3;//Held!
		
		//entra na SC 
		criticalSection();
		
		
	}

	public static void criticalSection() {

		System.out.println("Segurando a execucao");

		//um jeito de dar sleep
		try { Thread.sleep (2000); } catch (InterruptedException ex) {}
	}

	//sai da SC 
	public static void exitCriticalSection() {
		//muda o estado 
		AgrawaRicart.estado = 1;//Released!
		AgrawaRicart.replies = 0;
		
		
		//se a lista de processos for nao vazia, ele manda mensagem TCP pro proximo processo 
		//TCPClientThread tcpclientThread = new TCPClientThread(comandos[0],AgrawaRicart.id + "0OK0");
		if(!cs_list.isEmpty()){
			//Process menorTimestamp = new Process("menor", 1000000000);
			System.out.println("Tamanho lista " + cs_list.size());
			for (Process it : cs_list) {
				System.out.println("Lista: "+it.id);           
				TCPClientThread tcpclientThread = new TCPClientThread(it.id,AgrawaRicart.id+"NOKN");
				
			}

			cs_list.clear();
       		
			/*
			System.out.println("Removeu da lsita: "+menorTimestamp.id);
			
			cs_list.remove(menorTimestamp);
			*/
        }
		
		

		
	}
}




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
	Random rand = new Random();

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
			/*byte [] m = comandoterminal.getBytes();
			
			//mensagem que eu vou enviar 
			DatagramPacket messageOut = new DatagramPacket(m, m.length, group, 6789);

			//aqui que ele envia a mensagem pro grupo 
			s.send(messageOut);	

			//tem que separar os comandos com um caractere que nao de problema
			//no caso escolhido foi o 0
			comandos =  comandoterminal.split("0");
			id = comandos[0];*/
			
			//fica escutando o canal multicast 
			while(true){
				DatagramPacket messageIn = new DatagramPacket(buffer, buffer.length);
				s.receive(messageIn);
				mensagem = new String(messageIn.getData());
				comandos = mensagem.split("N");
				//System.out.println("comando: " + comandos[1]);
				System.out.println(comandos[0] + " e " +comandos[1] + " e " + comandos[2]);
				String temp1 = comandos[0];

				if (temp1.equals(AgrawaRicart.id)) {
					//System.out.println("UEEEE");
				}else{
					if (comandos[1].equals("request")){//chega uma requisicao
					
					
						if (!comandos[0].equals(id)) {//se o id da requisicao eh diferente do meu 
							//System.out.println("replies: " + AgrawaRicart.estado);
							if (AgrawaRicart.estado == 1) {//Released
								//manda unicast pro alvo
								//pra nao ser tipo B ouviu e vai responder pra B 
								System.out.println(AgrawaRicart.id + " ouviu em multicast e vai responder em unicast(TCP) pra " + comandos[0]);
								//TCPClientThread tcpclientThread = new TCPClientThread("destino","mensagem");
								TCPClientThread tcpclientThread = new TCPClientThread(comandos[0],AgrawaRicart.id+"NOKN");
								
							} 
	
							if (AgrawaRicart.estado == 2) {//wanted
								if (Integer.parseInt(comandos[2]) < AgrawaRicart.timestamp) {
									//responde imediatamente via TCP
									System.out.println("mandando mensagem no wanted " + comandos[2] + " e " + AgrawaRicart.timestamp);
									TCPClientThread tcpclientThread = new TCPClientThread(comandos[0],AgrawaRicart.id+"NOKN");
								
								}else{
									//bota na fila
									/* Se o meu estado eh Wanted ou Held, eu vou adicionar a uma lista os processos que querem entrar */
									System.out.println("Adicionou a fila: " + comandos[0]);
									AgrawaRicart.cs_list.add(new Process(comandos[0], Integer.parseInt(comandos[2])));
								}
	
							} 
		
							if (AgrawaRicart.estado == 3) {//held
								//bota na fila 
								System.out.println("Adicionou a fila: " + comandos[0]);
								AgrawaRicart.cs_list.add(new Process(comandos[0], Integer.parseInt(comandos[2])));
							} 
						} 
	
						
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
			String []comandos = data.split("N");
			if(!AgrawaRicart.id.equals(comandos[0])){
				System.out.println("Mensagem recebida de um cliente TCP: " + data);
			
				if(comandos[1].equals("OK")) {
					AgrawaRicart.replies +=1;
					System.out.println("numero de replies: " + AgrawaRicart.replies);
				}
				if(AgrawaRicart.replies == 2) {
					synchronized (AgrawaRicart.lock) {
						AgrawaRicart.lock.notify();
					}
				}
			}
			
			out.writeUTF("teste");
			//out.writeUTF(data);
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
			//System.out.println("servidor recebeu!");
		}catch (UnknownHostException e){System.out.println("Socket:"+e.getMessage());
		}catch (EOFException e){System.out.println("EOF:"+e.getMessage());
		}catch (IOException e){System.out.println("readline:"+e.getMessage());
		}finally {if(s!=null) try {s.close();}catch (IOException e){System.out.println("close:"+e.getMessage());}}

	}
}

public class Process {
	String id;
	int timestamp;
	Process(String id, int timestamp){
		this.id = id;
		this.timestamp = timestamp;
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

//https://www.guj.com.br/t/funcao-sleep/33034
//https://stackoverflow.com/questions/16758346/how-pause-and-then-resume-a-thread