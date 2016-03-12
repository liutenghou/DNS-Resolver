import java.net.*;
import java.io.*;
import java.util.*;

/**
 * 
 */

/**
 * @author Donald Acton
 * This example is adapted from Kurose & Ross
 *
 */
public class DNSlookup {


	static final int MIN_PERMITTED_ARGUMENT_COUNT = 2;
	static boolean tracingOn = false;
	static InetAddress rootNameServer;
	static byte[] sessionUid;
	static String recordName;
	static InetAddress recordValue;
	static int recordType;
	static byte[] query;
	static DNSResponse response;
	static int ttl; //find answer TTL
	static String finalIP; //the final answer IP address
	static String cname;

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		//initialize variables
		String fqdn = "";
		ttl = -4;
		finalIP = "0.0.0.0";
		byte query[]; 

		int argCount = args.length;
		
		if (argCount < 2 || argCount > 3) {
			usage();
			return;
		}
		
		rootNameServer = InetAddress.getByName(args[0]); //gets back InetAddress object
		
		fqdn = args[1];
		recordName = fqdn;
		//System.out.println(fqdn); //test, TODO: remove 
		//TODO: check if Additional Information is 0
		//TODO: possible for response to have wrong ID?
		
		//3 arguments, trace on
		if (argCount == 3 && args[2].equals("-t")){ //TODO: add check for -x where x != t
			tracingOn = true;
		}	
		
		boolean retry = false;
		int retryCount = 0;
		do{
			retry = false;
			try{
				//for the first query
				query = createQuery(fqdn);
				sendQuery(rootNameServer, query);
				ttl = response.getTtl();
				finalIP = recordValue.getHostAddress();
				cname = fqdn; //this will be the original fqdn, until we need to change it

				//check type that we get back getRecordType()
				//send something back depending on first level
				int count = 0;
				while(count < 30){
					//keep sending queries until we get a Type A response, or 30 requests
					count++;
					if(count >= 30){
						ttl = -3;
						finalIP = "0.0.0.0";
						break;
					}
					
					//if CNAME, record type == 5
					if(recordType == 5){
						cname = response.getRecordName();
						query = createQuery(cname); //create another query with new domain name
						sendQuery(rootNameServer, query);
						ttl = response.getTtl();
					} else{						
						//If there is no additional information, then send another query with 
						//name of the ns server
						if(recordValue == null){
							cname = response.getCNAME();
							query = createQuery(cname); //create another query with new domain name
							sendQuery(rootNameServer, query);
						} else if(recordType == 1 && recordName.equals(fqdn)){
							//if we get a type A response and it's for our original query, then we're done
							ttl = response.getTtl();
							finalIP = recordValue.getHostAddress();
							break;
						} else if(recordType == 1 && !recordName.equals(fqdn)){
							//we may have had to query the IP of an ns server, if we get it back then
							//we re-query the orginal fqdn with the answer we got
							cname = fqdn;
							query = createQuery(fqdn);
							sendQuery(recordValue, query);
						} else{
							//otherwise keep sending queries either with the original fqdn, or the ns server name
							query = createQuery(cname);
							sendQuery(recordValue, query);							
						}
						
					}
				}				
			} catch(NullPointerException e){ //check if there is no ip for domain requested
				if(recordType == -1 && (response.getReplyCode() == 3)){
					ttl = -1;
					finalIP = "0.0.0.0";
				}
				//TODO: remove below
				System.out.println("NULL POINTER: "+e.getLocalizedMessage());
				e.printStackTrace();
			}catch(SocketTimeoutException e){ //timeout waiting for response from root
				//retry the query 1 more time if timeout
				retry = true;
				retryCount++;
				ttl = -2;
				finalIP = "0.0.0.0";
				System.out.println("SocketTimeOut: "+e.getLocalizedMessage());
			}catch(Exception e){
				//TODO: remove below
				System.out.println("TYPE: " + e.getClass().getName());
				System.out.println("ERROR: " + e.getMessage());
			}
		} while(retryCount <= 1 && retry == true); //allow 1 retry in case of timeout
		
		//print out final answer
		System.out.println(fqdn + " " + ttl + " " + finalIP);
	}

	private static void sendQuery(InetAddress server, byte[] query) throws IOException{
		DatagramSocket socket = new DatagramSocket();
		DatagramPacket packetSent = new DatagramPacket(query, query.length);
		int responseSize;
		DatagramPacket packetReceived;
		byte[] data;
		
		socket.setSoTimeout(5000); //timeout if waiting for more than 4 seconds for response
		socket.connect(server,53);	
		socket.send(packetSent); //sends the byte packet

		responseSize = socket.getReceiveBufferSize();
		data = new byte[responseSize];
		packetReceived = new DatagramPacket(data, responseSize);
		socket.receive(packetReceived);

		//FOR DEBUGGING, delete/modify below this line
		response = new DNSResponse(packetReceived.getData(), responseSize, server, tracingOn);
		recordType = response.getRecordType();
		//TODO: check these response values
		recordName = response.getRecordName();
		if(recordType != 5){
			ttl = response.getTtl();
			recordValue = response.getIPaddr(); //TODO: this should probably be moved up to the main method
			System.out.println("*********recordValue*********: " + recordValue);
		}
		
		//System.out.format("4DEBUG: %-30s , ttl: %-10d , record type: %-4s %s\n", recordName, ttl, recordType, recordValue);
	}

	//create a properly formatted query
	private static byte[] createQuery(String fqdn){
		byte[] uid = createUID();
		//set the current uid that we are using, so we can check it later
		sessionUid = uid;
		
		byte[] qnameArray = createQname(fqdn);
		
		//16 bytes + length of 
		byte[] query = new byte[16 + qnameArray.length];

		//initiate all items in the array to 0x0
		for(int i = 0; i < query.length; i++){
			query[i] = (byte)0;
		}

		//fill in UID in the first 2 items
		for(int i = 0; i < 2; i++){
			query[i] = uid[i];
		}

		//the rest of the bytes can all be 0x0
		//fill in the domain name at the correct position
		for(int i = 0; i < qnameArray.length; i++){
			query[i + 12] = qnameArray[i];
		}

		query[5] = (byte)1; //set the number of questions to 1
		query[query.length - 3] = (byte)1; //set the QTYPE to 1 (type A)
		query[query.length - 1] = (byte)1; //set the QCLASS to 1 (IN)

		//System.out.println("query: " + Arrays.toString(query)); //test, TODO: remove
		return query;
	}

	//Create a random 2-byte UID
	private static byte[] createUID(){
		//TODO: fix random, maybe
		Random random = new Random(); //can't get time to work for some reason
		byte[] bytes = new byte[2];

		random.nextBytes(bytes);

		return bytes;
	}

	//encode the domain name into a byte[]
	private static byte[] createQname(String fqdn){
		//Create an array list to hold our byte array
		ArrayList<Byte> byteList = new ArrayList<Byte>();

		//Split the fqdn into individual labels/parts
		String[] fqdnParts= fqdn.split("\\.");

		for(int i = 0; i < fqdnParts.length; i++){

			//Convert each part into a byte[]
			byte[] part = fqdnParts[i].getBytes();

			//add the length of that byte array to the arraylist
			byteList.add((byte)part.length);
			//add the part/label
			for(int j = 0; j < part.length; j++){
				byteList.add(part[j]);
			}
		}

		//signify the end of the qname by adding a zero length byte
		byteList.add((byte)0);

		//convert the arraylist into byte[] and return
		byte[] bytes = new byte[byteList.size()];
		for(int i = 0; i < byteList.size(); i++){
			bytes[i] = byteList.get(i);
		}

		return bytes;
	}

	private static void usage() {
		System.out.println("Usage: java -jar DNSlookup.jar rootDNS name [-t]");
		System.out.println("   where");
		System.out.println("       rootDNS - the IP address (in dotted form) of the root");
		System.out.println("                 DNS server you are to start your search at");
		System.out.println("       name    - fully qualified domain name to lookup");
		System.out.println("       -t      -trace the queries made and responses received");
	}
}


