package test.gov.nih.nci.hpc.web;

import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;

public class ClientResponseLoggingFilter implements ClientResponseFilter {

	@Override
	public void filter(final ClientRequestContext reqCtx, final ClientResponseContext resCtx) throws IOException {
		System.out.println("status: " + resCtx.getStatus());
		System.out.println("date: " + resCtx.getDate());
		System.out.println("last-modified: " + resCtx.getLastModified());
		System.out.println("location: " + resCtx.getLocation());
		System.out.println("headers:");
		for (Entry<String, List<String>> header : resCtx.getHeaders().entrySet()) {
			System.out.print("\t" + header.getKey() + " :");
			for (String value : header.getValue()) {
				System.out.print(value + ", ");
			}
			System.out.print("\n");
		}
		// System.out.println("media-type: " + resCtx.getMediaType().getType());
	}

}