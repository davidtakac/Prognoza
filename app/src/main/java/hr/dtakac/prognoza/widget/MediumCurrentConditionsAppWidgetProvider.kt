package hr.dtakac.prognoza.widget

import android.content.Context
import android.widget.RemoteViews
import hr.dtakac.prognoza.R
import hr.dtakac.prognoza.common.utils.formatPrecipitationTwoHours
import hr.dtakac.prognoza.model.ui.widget.CurrentConditionsWidgetUiModel

open class MediumCurrentConditionsAppWidgetProvider : SmallCurrentConditionsAppWidgetProvider() {
    override val widgetLayoutId: Int
        get() = R.layout.app_widget_current_conditions_medium
    override val widgetErrorLayoutId: Int
        get() = R.layout.app_widget_current_conditions_medium_empty

    override fun onSuccess(
        views: RemoteViews,
        context: Context?,
        uiModel: CurrentConditionsWidgetUiModel
    ) {
        super.onSuccess(views, context, uiModel)
        views.setTextViewText(
            R.id.tv_precipitation_forecast,
            context?.formatPrecipitationTwoHours(
                uiModel.precipitationTwoHours,
                uiModel.displayDataInUnit,
                significantPrecipitationColor = false
            )
        )
    }
}