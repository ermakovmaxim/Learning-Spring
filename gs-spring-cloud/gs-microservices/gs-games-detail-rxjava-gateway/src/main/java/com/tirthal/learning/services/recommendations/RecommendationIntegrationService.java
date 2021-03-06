package com.tirthal.learning.services.recommendations;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.ObservableExecutionMode;

import rx.Observable;
import rx.Subscriber;
import rx.schedulers.Schedulers;

@Service
public class RecommendationIntegrationService {

	private static final Logger _log = LoggerFactory.getLogger(RecommendationIntegrationService.class);
	
	@Autowired
	RestTemplate restTemplate;
	
	@Autowired
	Environment env;

	@HystrixCommand(fallbackMethod="getRecommendationsFallback", observableExecutionMode=ObservableExecutionMode.EAGER)
	public Observable<List<Game>> getRecommendations(final String glId) {
		return Observable.create(new Observable.OnSubscribe<List<Game>>() {						
			
			@Override
			public void call(Subscriber<? super List<Game>> observer) {
				try {
					if(!observer.isUnsubscribed()) {
						ParameterizedTypeReference<List<Game>> responseType = new ParameterizedTypeReference<List<Game>>() {  }; 
						
						_log.info("*** (3) Calling recommendation service");
						List<Game> recommendationResponse = restTemplate.exchange(env.getProperty("gs.games.recommendations.service.rest.endpoint"), HttpMethod.GET, null, responseType, glId).getBody();																		
						_log.info("*** (3) Got response from recommendation service");
						
						observer.onNext(recommendationResponse);													
						observer.onCompleted();
					}
				} catch(Exception e) {					
					observer.onError(e);
				}
			}			
		}).subscribeOn(Schedulers.newThread());		// Changed to non-blocking approach
	}
	
	@SuppressWarnings("unused")
	private Observable<List<Game>> getRecommendationsFallback(final String glId) {
		List<Game> fallbackRecommendation = new ArrayList<>();
		Game g = new Game();
		g.setGlId("G000");
		g.setTitle("No Recommendation");
		fallbackRecommendation.add(g);		
		return Observable.just(fallbackRecommendation);
	}
}
