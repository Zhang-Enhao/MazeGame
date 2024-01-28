import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


public class Tracker implements TrackerInterface {

	private String trackerIP;

	private String trackerPort;

	private static int N;

	private static int K;

	private Map<String, PlayerInterface> playerMap = new ConcurrentHashMap();
	
	@Override
	public String getTrackerIP() throws RemoteException {
		return trackerIP;
	}
	@Override
	public void setTrackerIP(String trackerIP) throws RemoteException {
		this.trackerIP = trackerIP;
	}
	@Override
	public String getTrackerPort() throws RemoteException {
		return trackerPort;
	}
	@Override
	public void setTrackerPort(String trackerPort) throws RemoteException {
		this.trackerPort = trackerPort;
	}
	@Override
	public Integer getN() throws RemoteException {
		return N;
	}
	@Override
	public Integer getK() throws RemoteException {
		return K;
	}

	@Override
	public void setN(Integer n) throws RemoteException {
		N = n;
	}

	@Override
	public void setK(Integer k) throws RemoteException {
		K = k;
	}

	@Override
	public List<PlayerInterface> getPlayerList() throws RemoteException {
		return playerMap.values().stream().collect(Collectors.toList());
	}

	protected Tracker() throws RemoteException {
		super();
	}

	@Override
	public boolean playerExists(String playerID) throws RemoteException{

		return playerMap.containsKey(playerID);
	}

	@Override
	public synchronized void updatePlayerList(ArrayList<PlayerInterface> playerList) throws RemoteException {
		playerMap.clear();
		for (PlayerInterface p : playerList) {
			playerMap.put(p.getPlayerID(), p);
		}
	}


	@Override
	public synchronized List<PlayerInterface> joinGame(PlayerInterface newPlayer) throws RemoteException {
		if (playerExists(newPlayer.getPlayerID())) {
			System.out.println(String.format("Player %s already exists", newPlayer.getPlayerID()));
			return new ArrayList<>();
		} else {
			playerMap.put(newPlayer.getPlayerID(), newPlayer);
			System.out.println(String.format("Player %s added successfully", newPlayer.getPlayerID()));
			List<PlayerInterface> playerList = getPlayerList();
			System.out.println(playerList.size());
			return playerList;
		}
	}

	@Override
	public void exitGame(String playerID) throws RemoteException {
		playerMap.remove(playerID);
		System.out.println(String.format("Player %s removed successfully", playerID));
	}

	// @Override
	// public synchronized void assignServer(String playerId, String serverType) throws RemoteException {
	// 	playerList.stream().filter(p -> p.serverType.equals(serverType)).findAny().ifPresent(p -> p.serverType = "");
	// 	playerList.stream().filter(p -> p.playerID.equals(playerId)).findAny().ifPresent(p -> p.serverType = serverType);
	// }


	public static void main(String[] args){
		if(args.length != 3 ) {
			System.err.println("Should have exactly 3 arguments");
			return;
		}

		try {
			int port = Integer.parseInt(args[0]);
			N = Integer.parseInt(args[1]);
			K = Integer.parseInt(args[2]);

			TrackerInterface tracker = new Tracker();
			TrackerInterface iTracker = (TrackerInterface) UnicastRemoteObject.exportObject(tracker, port);
			Registry registry = LocateRegistry.createRegistry(port);
			registry.rebind("Tracker", iTracker);
			System.out.println("Register Tracker Successfully!");
		} catch (Exception e) {
			System.err.println("Tracker exception: " + e.toString());
			e.printStackTrace();
		}


    }

}