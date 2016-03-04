import java.net.*;
import java.io.*;
import java.util.Random;
import java.util.ArrayList;
import java.net.DatagramPacket;

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
<<<<<<< HEAD
	//byte array corresponding to a DNS query
	
=======
	static byte[] sessionUid;

>>>>>>> 1717d5afbc1178518b8a444749100792ee48e901
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		String fqdn;
		byte query[]; 
		//create 2 byte random number QueryID
		
		//1 byte
		
		int argCount = args.length;
		
		if (argCount < 2 || argCount > 3) {
			usage();
			return;
		}

		rootNameServer = InetAddress.getByName(args[0]);
<<<<<<< HEAD
=======
		fqdn = args[1]; //fqdn = fully qualified domain name
		System.out.println(fqdn); //Testcode, remove
>>>>>>> 1717d5afbc1178518b8a444749100792ee48e901

		fqdn = args[1];
		
		
		sendQuery(rootNameServer, createQuery(fqdn));
<<<<<<< HEAD
		
		//check type that we get back getRecordType()
		//send something back depending on first level
		/*
		while(recordtyp != 1){
			keep sending queries until we get a Type A response

			if(tracingOn) print out each step
		}

		print out final answer
		*/
=======
>>>>>>> 1717d5afbc1178518b8a444749100792ee48e901
		
		//3 arguments, trace on
		if (argCount == 3 && args[2].equals("-t"))
			tracingOn = true;
		
	}

	

	private static void sendQuery(InetAddress server, byte[] query) throws IOException{
		DatagramSocket socket = new DatagramSocket();
		DatagramPacket packetSent = new DatagramPacket(query, query.length);
		int responseSize;
		DatagramPacket packetReceived;
		byte[] data;

		socket.connect(server,53);		

		socket.send(packetSent);

		responseSize = socket.getReceiveBufferSize();
		data = new byte[responseSize];
		packetReceived = new DatagramPacket(data, responseSize);
		socket.receive(packetReceived);

		//FOR DEBUGGING, delete/modify below this line
		DNSResponse response = new DNSResponse(packetReceived.getData(), responseSize);
		
		//response values
		String recordName = response.getRecordName();
		//check that these are right
		System.out.println(recordName);
		int ttl = response.getTtl();
		int recordType = response.getRecordType();
		InetAddress recordValue = response.getIPaddr();

		System.out.format("       %-30s , ttl: %-10d , record type: %-4s %s\n", recordName, ttl, recordType, recordValue);
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

		return query;
	}

	//Create a random 2-byte UID
	private static byte[] createUID(){

		Random random = new Random(5); //can't get time to work for some reason
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


