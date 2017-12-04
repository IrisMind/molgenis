package org.molgenis.oneclickimporter.exceptions;

import org.molgenis.data.CodedRuntimeException;
import org.molgenis.oneclickimporter.SheetType;

import java.text.MessageFormat;

import static org.molgenis.data.i18n.LanguageServiceHolder.getLanguageService;

public class EmptySheetException extends CodedRuntimeException
{
	private static final String ERROR_CODE = "OCI01";
	private SheetType sheettype;
	private String fileName;

	public EmptySheetException(SheetType sheettype, String fileName)
	{
		super(ERROR_CODE);
		this.sheettype = sheettype;
		this.fileName = fileName;
	}

	@Override
	public String getMessage()
	{
		return String.format("sheettype:%s fileName:%s", sheettype, fileName);
	}

	@Override
	public String getLocalizedMessage()
	{
		String label = sheettype == SheetType.EXCELSHEET ? "Excel sheet" : "CSV file";
		return getLanguageService().map(languageService ->
		{
			String format = languageService.getString(ERROR_CODE);
			return MessageFormat.format(format, label, fileName);
		}).orElse(super.getLocalizedMessage());
	}
}
