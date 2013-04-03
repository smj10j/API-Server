package com.smj10j.util;

import java.io.StringReader;
import java.io.Writer;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import com.smj10j.conf.Constants;
import com.smj10j.conf.FatalException;
import com.smj10j.jaxb.ResponseType;
import com.smj10j.model.APIRequest;

public abstract class JAXBUtil {

	public static <T> void marshal(APIRequest request, JAXBElement<T> responseRoot, Writer writer) throws FatalException {
		try {
			//setup
			JAXBContext jaxbContext = JAXBContext.newInstance(Constants.Path.JaxbPackage);
			Marshaller marshaller = jaxbContext.createMarshaller();
			
			//options
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

			//do the marshalling
			//marshaller.marshal(response.get(), writer);

			//marshalls with Response as the root element
			marshaller.marshal( responseRoot, writer);

			
		} catch (JAXBException e) {
			throw new FatalException(e);
		}
	}
	
	public static ResponseType unmarshal(String xmlResponse) throws FatalException {
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance(Constants.Path.JaxbPackage);
				
			Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

			JAXBElement<ResponseType> apiResponse = unmarshaller.unmarshal(
					new StreamSource( 
							new StringReader(
									xmlResponse
							)
					),
					ResponseType.class
			);
			return apiResponse.getValue();
						
		} catch (JAXBException e) {
			throw new FatalException(e);
		}

	}
}
