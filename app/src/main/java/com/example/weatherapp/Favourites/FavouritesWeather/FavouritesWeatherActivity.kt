package com.example.weatherapp.Favourites.FavouritesWeather


import android.os.Build
import android.os.Bundle
import android.widget.ProgressBar
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.weatherapp.Favourites.FavouritesViewModel
import com.example.weatherapp.Favourites.FavouritesViewModelFactory
import com.example.weatherapp.Home.ForecastAdapter
import com.example.weatherapp.Home.WeatherAdapter
import com.example.weatherapp.Model.CurrentWeather
import com.example.weatherapp.Model.CurrentWeatherState
import com.example.weatherapp.Model.SettingsInPlace
import com.example.weatherapp.Model.WeatherForecastState
import com.example.weatherapp.Model.WeatherRepository
import com.example.weatherapp.Model.WeatherResponse
import com.example.weatherapp.Network.WeatherRemoteDataSource
import com.example.weatherapp.WeatherDatabase.WeatherDatabase
import com.example.weatherapp.WeatherDatabase.WeatherLocalDataSource
import com.example.weatherapp.app_utils.FilterUtils
import com.example.weatherapp.app_utils.InternetConnectionUtil
import com.example.weatherapp.databinding.ActivityFavouritesWeatherBinding
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class FavouritesWeatherActivity : AppCompatActivity() {

    private val TAG = "FavouritesWeatherLog"

    // Weather
    private lateinit var binding: ActivityFavouritesWeatherBinding
    private lateinit var viewModel: FavouritesViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: WeatherAdapter
    private lateinit var recyclerViewForecast: RecyclerView
    private lateinit var adapterForecast: ForecastAdapter

    // Date & Time
    @RequiresApi(Build.VERSION_CODES.O)
    private val currentDateTime: LocalDateTime = LocalDateTime.now()

    @RequiresApi(Build.VERSION_CODES.O)
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    @RequiresApi(Build.VERSION_CODES.O)
    private val formattedDateTime = currentDateTime.format(formatter)

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFavouritesWeatherBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.progressBar.visibility = ProgressBar.GONE

        val viewModelFactory = FavouritesViewModelFactory(
            WeatherRepository.getInstance(
                WeatherLocalDataSource(
                    WeatherDatabase.getInstance(this).forecastDao(),
                    WeatherDatabase.getInstance(this).alertDao(),
                    WeatherDatabase.getInstance(this).currentWeatherDao()
                ), WeatherRemoteDataSource.getInstance()
            )
        )
        setupRecyclerViews()

        viewModel = ViewModelProvider(this, viewModelFactory).get(FavouritesViewModel::class.java)

        val fav = intent.getSerializableExtra("cords") as CurrentWeather
        if (InternetConnectionUtil.isInternetAvailable(this)) {
            viewModel.fetchWeatherDataFavourites(fav.lat, fav.lon)
            viewModel.fetchForecastDataFavourites(fav.lat, fav.lon)

        } else {
            viewModel.getFavouriteForecastDataFromLocal(fav.id)
            viewModel.getFavouriteWeatherDateFromLocal(fav)
        }



        lifecycleScope.launch {

            viewModel.weather.collect {state->
                when (state) {
                    is CurrentWeatherState.Loading -> {
                        binding.progressBar.visibility = ProgressBar.VISIBLE
                    }
                    is CurrentWeatherState.Success -> {
                        binding.progressBar.visibility = ProgressBar.GONE
                        updateUI(state.currentWeatherResponse)
                    }
                    is CurrentWeatherState.Error -> {
                        binding.progressBar.visibility = ProgressBar.GONE
                        viewModel.getFavouriteWeatherDateFromLocal(fav)

                    }
                }

            }
        }

        lifecycleScope.launch {
            viewModel.forecast.collect { state ->

                when (state) {
                    is WeatherForecastState.Loading -> {
                        binding.progressBar.visibility = ProgressBar.VISIBLE
                    }
                    is WeatherForecastState.Success -> {
                        binding.progressBar.visibility = ProgressBar.GONE
                        if (state.forecastResponse?.list?.isNotEmpty() == true) {
                            adapter.updateData(FilterUtils.filterCurrentDayData(state.forecastResponse))
                            adapterForecast.updateData(FilterUtils.filterDailyData(state.forecastResponse))
                        }

                    }
                    is WeatherForecastState.Error -> {
                        binding.progressBar.visibility = ProgressBar.GONE
                        viewModel.getFavouriteForecastDataFromLocal(fav.id)
                    }
                }
            }
        }



//        viewModel.forecast.observe(this) {
//            if (it.list.isNotEmpty()) {
//                adapter.updateData(FilterUtils.filterCurrentDayData(it))
//                adapterForecast.updateData(FilterUtils.filterDailyData(it))
//            }
//        }
    }

    private fun setupRecyclerViews() {
        recyclerView = binding.threeHourForecast
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        adapter = WeatherAdapter(emptyList())
        recyclerView.adapter = adapter

        recyclerViewForecast = binding.recyclerViewForecast
        recyclerViewForecast.setHasFixedSize(true)
        recyclerViewForecast.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        adapterForecast = ForecastAdapter(emptyList())
        recyclerViewForecast.adapter = adapterForecast
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateUI(weather: WeatherResponse?) {
        // Update UI with weather data //val weather = viewModel.weather.value.currentWeatherResponse
        if (weather != null) {
            if (SettingsInPlace.unit == "metric") {
                binding.tvTemperature.text = "${weather.main?.temp?.toInt()}°C"
                binding.tvMaxTemp.text = "${weather.main?.temp_max?.toInt()}°C"
                binding.tvMinTemp.text = "${weather.main?.temp_min?.toInt()}°C"
                binding.tvWindValue.text = "${weather.wind?.speed.toString()} m/s"
            } else if (SettingsInPlace.unit == "imperial") {
                binding.tvTemperature.text = "${weather.main?.temp?.toInt()}°F"
                binding.tvMaxTemp.text = "${weather.main?.temp_max?.toInt()}°F"
                binding.tvMinTemp.text = "${weather.main?.temp_min?.toInt()}°F"
                binding.tvWindValue.text = "${weather.wind?.speed.toString()} m/h"
            } else {
                binding.tvTemperature.text = "${weather.main?.temp?.toInt()}°K"
                binding.tvMaxTemp.text = "${weather.main?.temp_max?.toInt()}°K"
                binding.tvMinTemp.text = "${weather.main?.temp_min?.toInt()}°K"
                binding.tvWindValue.text = "${weather.wind?.speed.toString()} m/s"
            }

            binding.tvCityName.text = weather.name
            binding.tvDateTime.text = formattedDateTime
            binding.tvWeatherDescription.text = weather.weather?.get(0)?.description

            binding.tvVisibilityValue.text = "${weather.visibility.toString()} m"
            binding.tvHumidityValue.text = "${weather.main?.humidity}%"
            binding.tvPressureValue.text = "${weather.main?.pressure.toString()} hPa"
            binding.tvCloudsValue.text = "${weather.clouds?.all.toString()}%"

            val iconUrl =
                "https://openweathermap.org/img/wn/${weather.weather?.get(0)?.icon}@2x.png"
            Glide.with(this).load(iconUrl).into(binding.ivWeatherIcon)
        }
    }
}
