//package edu.nus.mazegame.trackerservice;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

public interface TrackerInterface extends Remote {


	public List<PlayerInterface> joinGame(PlayerInterface newPlayer) throws RemoteException;

	// public void exitGame(PlayerInterface player) throws RemoteException;

	public void updatePlayerList(ArrayList<PlayerInterface> playerList) throws RemoteException;

	public boolean playerExists(String playerID) throws RemoteException;

	public String getTrackerIP() throws RemoteException;

	public void setTrackerIP(String trackerIP) throws RemoteException;

	public String getTrackerPort() throws RemoteException;

	public void setTrackerPort(String trackerPort) throws RemoteException;

	public Integer getN() throws RemoteException;

	public Integer getK() throws RemoteException;

	public void setN(Integer n) throws RemoteException;

	public void setK(Integer k) throws RemoteException;

	public List<PlayerInterface> getPlayerList() throws RemoteException;

	public void exitGame(String playerID) throws RemoteException;
}
