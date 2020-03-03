package ChatServer;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import javax.print.attribute.standard.Severity;
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
	private List<UserInfo> user_list = new LinkedList<>();//클라이언트 정보 저장
	private List<RoomInfo> room_list = new LinkedList<>();
	private StringTokenizer st;
	
	
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
					broadCast("user_vector_update/ ");//user_list 갱신 protocol
					
				} catch (IOException e) {
					System.out.println("클라이언트와의 연결 실패");
					e.printStackTrace();
				}
			}
		});
		th.start();
	}
	
	private void broadCast(String str) {//모든 클라이언트에게 메세지 보내기
		for (int i = 0; i < user_list.size(); i++) {
			user_list.get(i).sendMessage(str);
		}
	}
	
	class UserInfo extends Thread{
		private InputStream is;
		private DataInputStream dis;
		private OutputStream os;
		private DataOutputStream dos;
		
		private String NickName;
		private Socket user_sock;
		private String msg;
		
		UserInfo(Socket sock) {
			this.user_sock = sock;
			try {
				is = sock.getInputStream();
				dis = new DataInputStream(is);
				os = sock.getOutputStream();
				dos = new DataOutputStream(os);
				
				NickName = dis.readUTF();
				textArea.append(NickName +"님이 입장하셨습니다.\n");

				msg = "NewUser/"+NickName;
				
				synchronized (user_list) {
					//기존 유저에게 새로운 유저 전달
					broadCast(msg);
					// 새로운 유저에게 기존 유저 전달
					for (int i = 0; i < user_list.size(); i++) {
						msg = "OldUser/"+ user_list.get(i).NickName;
						sendMessage(msg);
					}
				}
			}
			catch(IOException e) {
				e.printStackTrace();
			}
		}
		
		@Override
		public void run() {
			while(true) {
				try {
					msg = dis.readUTF();
					inMessage(msg);
				}
				catch(IOException e) {
					e.printStackTrace();
				}
			}
		}
		private void inMessage(String str) {
			st = new StringTokenizer(str,"/");
			String protocol = st.nextToken();
			String Message = st.nextToken();
			
			if(protocol.equals("Chat")) {
				String room = Message;
				int roomIdx = Collections.binarySearch(room_list, room);
				Message = st.nextToken();
				if(roomIdx>=0) {//방이 존재할 때 해당 방에게 쪽지 돌림
					Message = "Chat/"+NickName+"/"+Message+"\n";
					room_list.get(roomIdx).roomBroadCast(Message);
				}
				textArea.append(NickName+": "+Message+"\n");
			}
			else if(protocol.equals("Note")) {
				String user = Message;
				String note = st.nextToken();
				System.out.println(note);
				for(int i=0;i<user_list.size();i++) {
					if(user.equals(user_list.get(i).NickName)) {
						note = "Note/"+NickName+"/"+note;
						user_list.get(i).sendMessage(note);
					}
				}
			}
			else if(protocol.equals("CreateRoom")) {
				if(Collections.binarySearch(room_list, Message) < 0) {//겹치는 방 이름이 없을 경우
					RoomInfo r = new RoomInfo(Message,this);//현재 클라이언트를 저장
					room_list.add(r);
					Collections.sort(room_list,(r1,r2)->r1.roomName.compareTo(r2.roomName));//알파벳순으로 정렬
					//protocol - NewRoom/방이름
					String newMessage = "NewRoom/"+Message;
					broadCast(newMessage);
					newMessage = "CreateRoom/"+Message;
					sendMessage(newMessage);
				}
				else {
					sendMessage("CreteRoomFail/ ");
				}
			}
		}
		
		private void sendMessage(String str) {//자신과 연결된 클라이언트에게 메세지 보내기
			try {
				dos.writeUTF(str);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	class RoomInfo implements Comparable<String>{

		private String roomName;
		private List<UserInfo> room_user_list = new LinkedList<>();
		public RoomInfo(String roomName,UserInfo u) {
			this.roomName = roomName;
			room_user_list.add(u);
		}
		@Override
		public int compareTo(String o) {
			return roomName.compareTo(o);
		}
		
		private void roomBroadCast(String str) {
			for(int i = 0 ; i <room_user_list.size();i++) {
				UserInfo  u = room_user_list.get(i);
				u.sendMessage(str);
			}
		}
	}
	
	public static void main(String[] args) {
		new Server();
	}

}

