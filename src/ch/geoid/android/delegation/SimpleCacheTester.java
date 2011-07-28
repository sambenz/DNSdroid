package ch.geoid.android.delegation;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.xbill.DNS.DClass;
import org.xbill.DNS.DNSKEYRecord;
import org.xbill.DNS.DNSSEC;
import org.xbill.DNS.ExtendedFlags;
import org.xbill.DNS.Flags;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.RRset;
import org.xbill.DNS.Rcode;
import org.xbill.DNS.Record;
import org.xbill.DNS.Resolver;
import org.xbill.DNS.ResolverConfig;
import org.xbill.DNS.Section;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.Type;
import org.xbill.DNS.security.DNSSECVerifier;

import android.util.Log;

/**
 * Name: CacheTester<br>
 * Description: Test DNSSEC behind an DNS cache<br>
 *
 * Three types of name server are possible:
 * 1) Recursive Name Server
 * 2) Non-Validating Security-Aware Recursive Name Server
 * 3) Validating Security-Aware Recursive Name Server
 *
 * Evil is #3 with wrong trusted-key!!
 *
 *
 * Creation date: Nov. 18, 2009<br>
 * $Id$
 * 
 * @author samuel.benz@switch.ch
 */
public class SimpleCacheTester {

	private final String TAG = "dnsdroid";
	
	/**
	 * Network addresses to test
	 * 
	 */
    private List<InetAddress> addresses;

    /**
     * be verbose
     * 
     */
    private boolean verbose = false;
    
    /**
     * Public Constructor
     * 
     */
    public SimpleCacheTester(){
    	setAddresses(new ArrayList<InetAddress>());
    }
    
	/**
	 * Set the addresses to test
	 * 
	 * @param addresses the addresses to set
	 */
	public void setAddresses(List<InetAddress> addresses) {
		this.addresses = addresses;
	}

	/**
	 * Add an address to test
	 * 
	 * @param addresse add an address
	 */
	public void addAddresse(InetAddress addresse) {
		this.addresses.add(addresse);
	}

	/**
	 * Get the addresses to test
	 * 
	 * @return the addresses
	 */
	public List<InetAddress> getAddresses() {
		return addresses;
	}

	/**
	 * be verbose
	 * 
	 * @param verbose the verbose to set
	 */
	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	/**
	 * is verbose
	 * 
	 * @return the verbose
	 */
	public boolean isVerbose() {
		return verbose;
	}
	
	/**
	 * run the tests on all addresses
	 *  
	 * @param name The name to test
	 * @throws Exception 
	 */
	public void testAll(Name name) throws Exception {
		// get system resolver
		if(this.getAddresses().size() < 1){
			String[] hostname = ResolverConfig.getCurrentConfig().servers();
			if(hostname != null){	
				for(String host : hostname){
					this.addAddresse(InetAddress.getByName(host));
				}
			}
		}
		// use local host a resolver 
		if(this.getAddresses().size() < 1){
			this.addAddresse(InetAddress.getLocalHost());
		}
		// test all ip(s)
		for(InetAddress ip : this.getAddresses()){
			this.test(ip,name);
		}
	}
	
	/**
	 * run the test on one address
	 * 
	 * @param ip Resolver to test
	 * @param name The name to test
	 * @throws Exception 
	 */
	public void test(InetAddress ip,Name name) throws Exception {
		if(this.isVerbose()){
			Log.d(TAG,"Start dnssec test with address " + ip);
		}
				
		Resolver res = new SimpleResolver(ip.getHostAddress());
		res.setEDNS(0, 0, ExtendedFlags.DO, null);

		DNSSECVerifier dnssec = new DNSSECVerifier();
		Record rec = Record.newRecord(name, Type.DNSKEY, DClass.IN);
		Message query = Message.newQuery(rec);
		query.getHeader().setFlag(Flags.CD);
		Message response = res.send(query);
		boolean sec_aware = true;		
		boolean self_sig = false;
		if(response.getHeader().getRcode() == Rcode.NOERROR){
			// check edns0 header in response
			if(response.getOPT() == null || response.getOPT().getFlags() != ExtendedFlags.DO){
				sec_aware = false;
				if(verbose){
					Log.d(TAG,"Info: Server " + ip + " does not understand the EDNS0 DO flag");
				}
			}
			// are rrsigs available
			for (RRset rrset : response.getSectionRRsets(Section.ANSWER)) {
				if (!rrset.sigs().hasNext()) {
					sec_aware = false;
					if(verbose){
						Log.d(TAG,"Info: No RRSIG received from " + ip + " for  qeury:" + query.sectionToString(Section.QUESTION));
					}
				}
			}
			if(sec_aware){
				// check self signature (with +cd)
				if(verbose){
					Log.d(TAG,"Info: Server is security aware!");
				}
				for(Record entry : response.getSectionArray(Section.ANSWER)){
					if(entry.getType() == Type.DNSKEY){
						dnssec.addTrustedKey((DNSKEYRecord) entry);
					}
				}
				for(RRset rrset : response.getSectionRRsets(Section.ANSWER)){
					if(dnssec.verify(rrset, null) == DNSSEC.Secure){
						self_sig = true;
						if(verbose){
							Log.d(TAG,"Info: Self signature is valid!");
						}
					}else{
						Log.d(TAG,"Error: Self signature is invald!");	
						throw new NoDNSSECException();
					}
				}
			}else{
				if(verbose){
					Log.d(TAG,"Info: Server is not security aware!");
				}
				throw new NoDNSSECException();
			}
		}else if(response.getHeader().getRcode() == Rcode.REFUSED){
			// do nothing !
			if(verbose){
				Log.d(TAG,"Error: Received " + Rcode.string(response.getHeader().getRcode()) + " from " + ip + " for qeury:" + query.sectionToString(Section.QUESTION));	
			}
		}else{
			Log.d(TAG,"Error: Received " + Rcode.string(response.getHeader().getRcode()) + " from " + ip + " for qeury:" + query.sectionToString(Section.QUESTION));	
			throw new NoDNSSECException();
		}
		
		
		// is server correct validating
		if(sec_aware && self_sig){
			rec = Record.newRecord(name, Type.SOA,DClass.IN);
			query = Message.newQuery(rec);
			response = res.send(query);
			if(response.getHeader().getRcode() == Rcode.NOERROR) {
				if(response.getHeader().getFlag(Flags.AD) && response.getSectionArray(Section.ANSWER).length > 0){
					if(verbose){
						Log.d(TAG,"Info: Validated answer received!");
					}
				}else if(response.getSectionArray(Section.ANSWER).length > 0){
					if(verbose){
						Log.d(TAG,"Info: Non-Validated answer received!");
					}
					throw new NoDNSSECException();
				}										
			}else if (response.getHeader().getRcode() == Rcode.SERVFAIL){
				Message cd_query = Message.newQuery(rec);
				cd_query.getHeader().setFlag(Flags.CD);
				Message cd_response = res.send(cd_query);
				if(cd_response.getHeader().getRcode() == Rcode.NOERROR) {
					if(cd_response.getSectionArray(Section.ANSWER).length > 0){
						Log.d(TAG,"Error: Possible trust-anschor problem detected! (Got answer only with +cd bit)");
						throw new NoDNSSECException();
					}
				}
			}else{
				Log.d(TAG,"Error: Received " + Rcode.string(response.getHeader().getRcode()) + " from " + ip + " for qeury:" + query.sectionToString(Section.QUESTION));	
				throw new NoDNSSECException();
			}
		}
	}

	/**
	 * The main method
	 * 
	 * return 0   -> everything works well
	 * return > 0 -> error
	 * 
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {
		SimpleCacheTester test = new SimpleCacheTester();
		String domain = "switch.ch";
		boolean catch_domain = false;
		for(String arg : args){
			if(catch_domain){
				domain = arg;
				catch_domain = false;
			}else if(arg.matches("-v") || arg.matches("--verbose")){
				test.setVerbose(true);
			}else if(arg.matches("-d") || arg.matches("--domain")){
				catch_domain = true;
			}else if(arg.matches("-h") || arg.matches("--help")) {
	            System.exit(1);
			}else {
	        	try {
					test.addAddresse(InetAddress.getByName(arg));
				} catch (UnknownHostException e) {
					System.err.println("Error: could not add address " + arg);
				}
			}
		}        		
        try {
        	test.testAll(new Name(domain,Name.root));
        }catch (Exception e){
        	if(test.isVerbose()){
        		e.printStackTrace();
        	}
        	System.err.println("Error: Test faild!");
        	System.exit(2);
        }   
	}
}
