package com.example;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousSocketChannel;

public class A{
	public AsynchronousSocketChannel client;
	public A(AsynchronousSocketChannel client){
		this.client=client;
	}
	public InetAddress test(){//target method
		try {
			if(client.getRemoteAddress() instanceof InetSocketAddress isa)
				return isa.getAddress();
		} catch (IOException e) {
			
		}
		return null;
	}
}