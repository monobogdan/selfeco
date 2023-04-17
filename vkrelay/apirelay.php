<?php
	ob_start();
	error_reporting(E_ALL);


	header("Content-Type: text/json");
	$request = file_get_contents("php://input");
	
	$curl = curl_init($request);
	curl_exec($curl);
	
	header("Content-length: " . ob_get_length());
	ob_flush();