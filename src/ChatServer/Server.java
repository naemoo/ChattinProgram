package ChatServer;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

public class Server extends JFrame implements ActionListener{
	
	//GUI resource
	private JPanel contentPane;
	private JTextField port_tf;
	private JTextArea textArea = new JTextArea();
	private JButton start_btn = new JButton("서버 실행");
	private JButton stop_btn = new JButton("서버 중단");
	
	//Network resource
	private int port;
	private ServerSocket server_sock;
	private Socket sock;
	
	//others
	private List<UserInfo> user_list = new LinkedList<>();
	
	
	public Server() {//생성자 
		init();
		startListen();
	}
	private void init() {//Server GUI 생성
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 408, 453);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);
		
		textArea.setBounds(12, 10, 366, 285);
		contentPane.add(textArea);
		
		JLabel lblNewLabel = new JLabel("포트 번호 :");
		lblNewLabel.setBounds(28, 317, 71, 15);
		contentPane.add(lblNewLabel);
		
		port_tf = new JTextField();
		port_tf.setBounds(111, 314, 267, 21);
		contentPane.add(port_tf);
		port_tf.setColumns(10);
		
		start_btn.setBounds(28, 357, 166, 23);
		contentPane.add(start_btn);
		
		stop_btn.setBounds(206, 357, 172, 23);
		contentPane.add(stop_btn);
		
		setVisible(true);
	}
	
	private void startListen() {//컴포넌트 메세지 처리
		start_btn.addActionListener(this);
		stop_btn.addActionListener(this);
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {//메세지 처리 함수
		if(e.getSource() == start_btn) {
			System.out.println("서버 실행 버튼");
			initListenSock();
			connetion();
		}
		if(e.getSource() == stop_btn) {
			System.out.println("서버 중단 버튼");
		}
	}
	
	private void initListenSock() {//Listen 소켓 초기화
		try {
			port = Integer.parseInt(port_tf.getText().trim());
			server_sock = new ServerSocket(port);
		}
		catch(NumberFormatException e) {
			System.out.println("잘못된 port 입력");
			e.printStackTrace();
		}
		catch (IOException e) {
			System.out.println("서버 소켓 생성 실패");
			e.printStackTrace();
		}
	}

	private void connetion() {//대기 큐에 있는 소켓 연결 - Thread 사용으로  Blocking 방지
		Thread th = new Thread(()-> {
			while(true) {
				try {
					textArea.append("사용자와 연결 대기 중\n");
					sock = server_sock.accept();
					textArea.append("사용자와 연결 성공\n");
					
					UserInfo user = new UserInfo(sock);
					user.start();
					
					user_list.add(user);
					
				} catch (IOException e) {
					System.out.println("클라이언트와의 연결 실패");
					e.printStackTrace();
				}
			}
		});
		th.start();
	}
	
	class UserInfo extends Thread{
		private InputStream is;
		private BufferedReader br;
		private OutputStream os;
		private BufferedWriter bw;
		
		private String NickName;
		private Socket user_sock;
		private String msg;
		
		UserInfo(Socket sock) {
			this.user_sock = sock;
			try {
				is = sock.getInputStream();
				br = new BufferedReader(new InputStreamReader(is));
				os = sock.getOutputStream();
				bw = new BufferedWriter(new OutputStreamWriter(os));
				NickName = String.valueOf(br.read());
				System.out.println(NickName);  
				textArea.append(NickName +"님이 입장하셨습니다.\n");
			}
			catch(IOException e) {
				e.printStackTrace();
			}
		}
		
		@Override
		public void run() {
			while(true) {
				try {
					msg = br.readLine();
					textArea.append(msg+"\n");
				}
				catch(IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		private void sendMessage(String str) {
			try {
				bw.write(str+"\n");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void main(String[] args) {
		new Server();
	}

}

