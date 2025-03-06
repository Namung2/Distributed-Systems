import Controller.UDP_storage;
import Connection.Connection2Primary;
import Repository.LocalRepository;

public class Main {
    public static void main(String[] args) {
        LocalRepository localRepository = LocalRepository.getInstance();
        // Connection2Primary 인스턴스를 생성하고 BackupController를 전달
        Connection2Primary connection2Primary = new Connection2Primary(localRepository);

        // TCP_storage 인스턴스를 생성하여 클라이언트의 요청을 처리
        UDP_storage udpStorage = new UDP_storage(connection2Primary);

        // 서버를 시작
        udpStorage.startStorage();

    }
}