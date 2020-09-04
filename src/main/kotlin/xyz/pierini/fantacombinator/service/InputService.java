package xyz.pierini.fantacombinator.service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import xyz.pierini.fantacombinator.model.input.CombinatorWrapper;

@Service
public class InputService {
	
	private final String JSON_WRAPPER_FILENAME = "fantacombinator.json";

	@Autowired
	private ObjectMapper objectMapper;

	public CombinatorWrapper getCombinatorWrapper() throws JsonMappingException, JsonProcessingException {
		InputStream is = getResourceFileAsInputStream(JSON_WRAPPER_FILENAME);
		if (is != null) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(is));
			String json = (String) reader.lines().collect(Collectors.joining(System.lineSeparator()));
			return objectMapper.readValue(json, CombinatorWrapper.class);
		} else {
			throw new RuntimeException("resource not found");
		}
	}

	private static InputStream getResourceFileAsInputStream(String fileName) {
		ClassLoader classLoader = CombinatorService.class.getClassLoader();
		return classLoader.getResourceAsStream(fileName);
	}

}
