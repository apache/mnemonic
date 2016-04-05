
package org.apache.mnemonic;

import java.io.File;

/**
 * a memory file that manages its data on native memory storage. Note: this
 * class depends on PMFS, we suggest that NVM library to support this feature in
 * native layer. In addition, the permission of /mnt/pmfs will be set properly.
 * 
 *
 */
public class MemFile extends File {

	private static final long serialVersionUID = 6579668848729471173L;
	private String uri, id;

	/**
	 * initialize the memory file.
	 * 
	 * @param uri
	 *            specify the location of memory file
	 * 
	 * @param id
	 *            specify the id of memory file
	 */
	public MemFile(String uri, String id) {
		super(uri, id);
		this.uri = uri;
		this.id = id;
	}

	/**
	 * retrieve the uri of this memory file.
	 * 
	 * @return the uri of memory file
	 */
	public String getUri() {
		return this.uri;
	}

	/**
	 * retrieve the id of this memory file.
	 * 
	 * @return the id of memory file
	 */
	public String getId() {
		return this.id;
	}

}
