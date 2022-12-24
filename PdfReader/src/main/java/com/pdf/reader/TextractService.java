package com.pdf.reader;

import java.io.FileNotFoundException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.springframework.stereotype.Component;
import org.w3c.dom.Element;

import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.textract.TextractClient;
import software.amazon.awssdk.services.textract.model.AnalyzeDocumentRequest;
import software.amazon.awssdk.services.textract.model.AnalyzeDocumentResponse;
import software.amazon.awssdk.services.textract.model.Block;
import software.amazon.awssdk.services.textract.model.Document;
import software.amazon.awssdk.services.textract.model.EntityType;
import software.amazon.awssdk.services.textract.model.FeatureType;
import software.amazon.awssdk.services.textract.model.Relationship;
import software.amazon.awssdk.services.textract.model.TextractException;


@Component
public class TextractService {
	
	
	
	 public String analyzeDoc(byte[] bytes) throws FileNotFoundException {
		 
	        List<String> myList = new ArrayList<String>();
	        try {
	            Region region = Region.US_EAST_2;
	            TextractClient textractClient = TextractClient.builder()
	                    .region(region).credentialsProvider(EnvironmentVariableCredentialsProvider.create())
	                    .build();

	            SdkBytes sourceBytes = SdkBytes.fromByteArray(bytes);

	            // Get the input Document object as bytes
	            Document myDoc = Document.builder()
	                    .bytes(sourceBytes)
	                    .build();

	            List<FeatureType> featureTypes = new ArrayList<FeatureType>();
	            featureTypes.add(FeatureType.FORMS);
	            featureTypes.add(FeatureType.TABLES);

	            AnalyzeDocumentRequest analyzeDocumentRequest = AnalyzeDocumentRequest.builder()
	                    .featureTypes(featureTypes)
	                    .document(myDoc)
	                    .build();

	            AnalyzeDocumentResponse analyzeDocument = textractClient.analyzeDocument(analyzeDocumentRequest);
	            List<Block> blocks = analyzeDocument.blocks();
//	            Iterator<Block> blockIterator = blocks.iterator();
	            
	            
	            Map<String, Block> blockMap = new LinkedHashMap<>();
	            Map<String, Block> keyMap = new LinkedHashMap<>();
	            Map<String, Block> valueMap = new LinkedHashMap<>();

	            for (Block b : blocks) {
	                String block_id = b.id();
	                blockMap.put(block_id, b);
	                if(b.blockTypeAsString().equals("KEY_VALUE_SET")) {
	                    for(EntityType entityType : b.entityTypes()) {
	                        if(entityType.toString().equals("KEY")) {
	                            keyMap.put(block_id, b);
	                        } else {
	                            valueMap.put(block_id, b);
	                        }
	                    }
	                }
	            }
//	                System.out.println(getRelationships(blockMap, keyMap, valueMap));
//	                textractClient.close();
	          

//	            while(blockIterator.hasNext()) {
	                myList.add(getRelationships(blockMap, keyMap, valueMap).toString());
//	            }
	            textractClient.close();

	            return convertToString(toXml(myList));
	     
	         } catch (TextractException  e) {

	            System.err.println(e.getMessage());
	            System.exit(1);
	        }

	        return "" ;
	      }

	      // Convert items into XML to pass back to the view.
	      private org.w3c.dom.Document toXml(List<String> itemList) {

	        try {
	            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	            DocumentBuilder builder = factory.newDocumentBuilder();
	            org.w3c.dom.Document doc = builder.newDocument();

	            // Start building the XML.
	            Element root = doc.createElement( "Items" );
	            doc.appendChild( root );

	            // Get the elements from the collection.
	            int custCount = itemList.size();

	            // Iterate through the collection.
	            for ( int index=0; index < custCount; index++) {

	                String itemValue = itemList.get(index);

	                Element item = doc.createElement( "Item" );
	                root.appendChild( item );

	                // Set Key.
	                Element id = doc.createElement( "Doc" );
	                id.appendChild( doc.createTextNode(itemValue) );
	                item.appendChild( id );
	            }

	            return doc;
	        } catch(ParserConfigurationException e) {
	            e.printStackTrace();
	        }
	        return null;
	       }

	      private String convertToString(org.w3c.dom.Document xml) throws FileNotFoundException {
	        try {
	            Transformer transformer = TransformerFactory.newInstance().newTransformer();
	            StreamResult result = new StreamResult(new StringWriter());
	            DOMSource source = new DOMSource(xml);
	            transformer.transform(source, result);
	            return result.getWriter().toString();

	         } catch(TransformerException ex) {
	            ex.printStackTrace();
	         }
	        return null;
	      }
	      
	      public static Map<String, String> getRelationships(Map<String, Block> blockMap, Map<String, Block> keyMap,Map<String, Block> valueMap) {
	          Map<String, String> result = new LinkedHashMap<>();
	          for(Map.Entry<String, Block> itr : keyMap.entrySet()) {
	              Block valueBlock = findValue(itr.getValue(), valueMap);
	              String key = getText(itr.getValue(), blockMap);
	              String value = getText(valueBlock, blockMap);
	              result.put(key, value);
	          }
	          return result;
	      }

	      public static Block findValue(Block keyBlock, Map<String, Block> valueMap) {
	          Block b = null;
	          for(Relationship relationship : keyBlock.relationships()) {
	              if(relationship.type().toString().equals("VALUE")) {
	                  for(String id : relationship.ids()) {
	                      b = valueMap.get(id);
	                  }
	              }
	          }
	          return b;
	      }

	      public static String getText(Block result, Map<String, Block> blockMap) {
	          StringBuilder stringBuilder = new StringBuilder();
	          for(Relationship relationship : result.relationships()) {
	              if(relationship.type().toString().equals("CHILD")) {
	                  for(String id : relationship.ids()) {
	                      Block b = blockMap.get(id);
	                      if(b.blockTypeAsString().equals("Line")||b.blockTypeAsString().equals("Table")||b.blockTypeAsString().equals("WORD")) {
	                          stringBuilder.append(b.text()).append(" ");
	                      }
	                   }
	              }
	          }
	          return stringBuilder.toString();
	      }

}
